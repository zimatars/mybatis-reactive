package com.waterdrop.mybatisreactive.executor.resultset;

import com.waterdrop.mybatisreactive.exception.ReactiveMybatisException;
import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import io.r2dbc.spi.Result;
import org.apache.ibatis.annotations.AutomapConstructor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.result.ResultMapException;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import reactor.core.publisher.Flux;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DefaultReactiveResultSetHandler implements ReactiveResultSetHandler{
//    private ResultSetHandler delegate;

    private static final Object DEFERRED = new Object();

    private final ReactiveExecutor executor;
    private final Configuration configuration;
    private final MappedStatement mappedStatement;
    private final RowBounds rowBounds;
    private final ReactiveParameterHandler parameterHandler;
    private final ResultHandler<?> resultHandler;
    private final BoundSql boundSql;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private final ObjectFactory objectFactory;
    private final ReflectorFactory reflectorFactory;

    // Cached Automappings
    private final Map<String, List<UnMappedColumnAutoMapping>> autoMappingsCache = new HashMap<>();

    // temporary marking flag that indicate using constructor mapping (use field to reduce memory usage)
    private boolean useConstructorMappings;

    private static class UnMappedColumnAutoMapping {
        private final String column;
        private final String property;
        private final TypeHandler<?> typeHandler;
        private final boolean primitive;

        public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
            this.column = column;
            this.property = property;
            this.typeHandler = typeHandler;
            this.primitive = primitive;
        }
    }

    public DefaultReactiveResultSetHandler(ReactiveExecutor executor, MappedStatement mappedStatement, ReactiveParameterHandler parameterHandler, ResultHandler<?> resultHandler, BoundSql boundSql,
                                           RowBounds rowBounds) {
//       new DefaultResultSetHandler(null,mappedStatement,parameterHandler,resultHandler)
//        delegate = new DefaultResultSetHandler(null, mappedStatement, null, resultHandler, boundSql, rowBounds);
        this.executor = executor;
        this.configuration = mappedStatement.getConfiguration();
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;
        this.parameterHandler = parameterHandler;
        this.boundSql = boundSql;
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.objectFactory = configuration.getObjectFactory();
        this.reflectorFactory = configuration.getReflectorFactory();
        this.resultHandler = resultHandler;
    }

    /*@Override
    public <E> List<E> handleResultSets(Iterator<List<RowWrap>> results) {
        Statement statementPoxy = (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                new Class[]{Statement.class}, new StatementProxy(results));
        try {
            return delegate.handleResultSets(statementPoxy);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }*/

    @Override
    public Flux<Object> handleResultSets(List results){
        List<Result> resultList = (List<Result>) results;
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        int resultMapCount = resultMaps.size();
        validateResultMapsCount(results.isEmpty(), resultMapCount);

        int resultSetNum = Math.min(resultMapCount, results.size());
        return Flux.range(0, resultSetNum).flatMap(index -> {
            ResultMap resultMap = resultMaps.get(index);
            Result result = resultList.get(index);
            ResultSetProxyHandler resultSetProxyHandler = new ResultSetProxyHandler();
            ResultSet proxyResultSet = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                    new Class[]{ResultSet.class}, resultSetProxyHandler);
            Flux<Object> r = Flux.from(result.map((row, rowMetadata) -> {
                resultSetProxyHandler.initRowInfo(row, rowMetadata);
                ResultSetWrapper rsw;
                try {
                    rsw = new ResultSetWrapper(proxyResultSet, this.configuration);
                    Object rowValue = getRowValue(rsw, resultMap, null);
                    return rowValue;
                } catch (SQLException exception) {
                    throw new ReactiveMybatisException(exception);
                }
            }));
            //collapseSingleResultList
            if(resultSetNum==1){
                return r;
            }else {
                return r.collectList();
            }
        });
    }

    private void validateResultMapsCount(boolean isEmpty, int resultMapCount) {
        if (!isEmpty && resultMapCount < 1) {
            throw new ExecutorException("A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
                    + "'.  It's likely that neither a Result Type nor a Result Map was specified.");
        }
    }

   /* private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults, ResultMapping parentMapping) throws SQLException {
        try {
            if (parentMapping != null) {
                handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
            } else {
                if (resultHandler == null) {
                    DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
                    handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
                    multipleResults.add(defaultResultHandler.getResultList());
                } else {
                    handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
                }
            }
        } finally {
            // issue #228 (close resultsets)
            closeResultSet(rsw.getResultSet());
        }
    }*/

    private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
        final ResultLoaderMap lazyLoader = new ResultLoaderMap();
        Object rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
        if (rowValue != null && !hasTypeHandlerForResultObject(resultMap.getType())) {
            final MetaObject metaObject = configuration.newMetaObject(rowValue);
            boolean foundValues = this.useConstructorMappings;
            if (shouldApplyAutomaticMappings(resultMap, false)) {
                foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
            }
            foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
            foundValues = lazyLoader.size() > 0 || foundValues;
            rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
        }
        return rowValue;
    }

    //
    // PROPERTY MAPPINGS
    //

    private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix)
            throws SQLException {
        final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        boolean foundValues = false;
        final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
        for (ResultMapping propertyMapping : propertyMappings) {
            String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
            if (propertyMapping.getNestedResultMapId() != null) {
                // the user added a column attribute to a nested result map, ignore it
                column = null;
            }
            if (propertyMapping.isCompositeResult()
                    || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)))
                    || propertyMapping.getResultSet() != null) {
                Object value = getPropertyMappingValue(rsw.getResultSet(), metaObject, propertyMapping, lazyLoader, columnPrefix);
                // issue #541 make property optional
                final String property = propertyMapping.getProperty();
                if (property == null) {
                    continue;
                } else if (value == DEFERRED) {
                    foundValues = true;
                    continue;
                }
                if (value != null) {
                    foundValues = true;
                }
                if (value != null || (configuration.isCallSettersOnNulls() && !metaObject.getSetterType(property).isPrimitive())) {
                    // gcode issue #377, call setter on nulls (value is not 'found')
                    metaObject.setValue(property, value);
                }
            }
        }
        return foundValues;
    }

    private Object getPropertyMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
            throws SQLException {
        if (propertyMapping.getNestedQueryId() != null) {
            //todo impl
            return null;
//            return getNestedQueryMappingValue(rs, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
        } else if (propertyMapping.getResultSet() != null) {
            //todo impl
//            addPendingChildRelation(rs, metaResultObject, propertyMapping);   // TODO is that OK?
            return DEFERRED;
        } else {
            final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
            final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
            return typeHandler.getResult(rs, column);
        }
    }


    private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
        this.useConstructorMappings = false; // reset previous mapping result
        final List<Class<?>> constructorArgTypes = new ArrayList<>();
        final List<Object> constructorArgs = new ArrayList<>();
        Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
        if (resultObject != null && !hasTypeHandlerForResultObject(resultMap.getType())) {
            final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
            for (ResultMapping propertyMapping : propertyMappings) {
                // issue gcode #109 && issue #149
                if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
                    resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
                    break;
                }
            }
        }
        this.useConstructorMappings = resultObject != null && !constructorArgTypes.isEmpty(); // set current mapping result
        return resultObject;
    }

    private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix)
            throws SQLException {
        final Class<?> resultType = resultMap.getType();
        final MetaClass metaType = MetaClass.forClass(resultType, reflectorFactory);
        final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
        if (hasTypeHandlerForResultObject(resultType)) {
            return createPrimitiveResultObject(rsw, resultMap, columnPrefix);
        } else if (!constructorMappings.isEmpty()) {
            return createParameterizedResultObject(rsw, resultType, constructorMappings, constructorArgTypes, constructorArgs, columnPrefix);
        } else if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
            return objectFactory.create(resultType);
        } else if (shouldApplyAutomaticMappings(resultMap, false)) {
            return createByConstructorSignature(rsw, resultType, constructorArgTypes, constructorArgs);
        }
        throw new ExecutorException("Do not know how to create an instance of " + resultType);
    }

    private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
        List<UnMappedColumnAutoMapping> autoMapping = createAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
        boolean foundValues = false;
        if (!autoMapping.isEmpty()) {
            for (UnMappedColumnAutoMapping mapping : autoMapping) {
                final Object value = mapping.typeHandler.getResult(rsw.getResultSet(), mapping.column);
                if (value != null) {
                    foundValues = true;
                }
                if (value != null || (configuration.isCallSettersOnNulls() && !mapping.primitive)) {
                    // gcode issue #377, call setter on nulls (value is not 'found')
                    metaObject.setValue(mapping.property, value);
                }
            }
        }
        return foundValues;
    }

    private List<UnMappedColumnAutoMapping> createAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
        final String mapKey = resultMap.getId() + ":" + columnPrefix;
        List<UnMappedColumnAutoMapping> autoMapping = autoMappingsCache.get(mapKey);
        if (autoMapping == null) {
            autoMapping = new ArrayList<>();
            final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
            for (String columnName : unmappedColumnNames) {
                String propertyName = columnName;
                if (columnPrefix != null && !columnPrefix.isEmpty()) {
                    // When columnPrefix is specified,
                    // ignore columns without the prefix.
                    if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
                        propertyName = columnName.substring(columnPrefix.length());
                    } else {
                        continue;
                    }
                }
                final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
                if (property != null && metaObject.hasSetter(property)) {
                    if (resultMap.getMappedProperties().contains(property)) {
                        continue;
                    }
                    final Class<?> propertyType = metaObject.getSetterType(property);
                    if (typeHandlerRegistry.hasTypeHandler(propertyType, rsw.getJdbcType(columnName))) {
                        final TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
                        autoMapping.add(new UnMappedColumnAutoMapping(columnName, property, typeHandler, propertyType.isPrimitive()));
                    } else {
                        configuration.getAutoMappingUnknownColumnBehavior()
                                .doAction(mappedStatement, columnName, property, propertyType);
                    }
                } else {
                    configuration.getAutoMappingUnknownColumnBehavior()
                            .doAction(mappedStatement, columnName, (property != null) ? property : propertyName, null);
                }
            }
            autoMappingsCache.put(mapKey, autoMapping);
        }
        return autoMapping;
    }

    private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
        if (resultMap.getAutoMapping() != null) {
            return resultMap.getAutoMapping();
        } else {
            if (isNested) {
                return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior();
            } else {
                return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
            }
        }
    }

    private boolean hasTypeHandlerForResultObject(Class<?> resultType) {
        return typeHandlerRegistry.hasTypeHandler(resultType);
    }

    private Object createPrimitiveResultObject(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
        final Class<?> resultType = resultMap.getType();
        final String columnName;
        if (!resultMap.getResultMappings().isEmpty()) {
            final List<ResultMapping> resultMappingList = resultMap.getResultMappings();
            final ResultMapping mapping = resultMappingList.get(0);
            columnName = prependPrefix(mapping.getColumn(), columnPrefix);
        } else {
            columnName = rsw.getColumnNames().get(0);
        }
        final TypeHandler<?> typeHandler = rsw.getTypeHandler(resultType, columnName);
        return typeHandler.getResult(rsw.getResultSet(), columnName);
    }

    private String prependPrefix(String columnName, String prefix) {
        if (columnName == null || columnName.length() == 0 || prefix == null || prefix.length() == 0) {
            return columnName;
        }
        return prefix + columnName;
    }

    Object createParameterizedResultObject(ResultSetWrapper rsw, Class<?> resultType, List<ResultMapping> constructorMappings,
                                           List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix) {
        boolean foundValues = false;
        for (ResultMapping constructorMapping : constructorMappings) {
            final Class<?> parameterType = constructorMapping.getJavaType();
            final String column = constructorMapping.getColumn();
            final Object value;
            try {
                if (constructorMapping.getNestedQueryId() != null) {
                    //todo impl
                    value = null;//getNestedQueryConstructorValue(rsw.getResultSet(), constructorMapping, columnPrefix);
                } else if (constructorMapping.getNestedResultMapId() != null) {
                    final ResultMap resultMap = configuration.getResultMap(constructorMapping.getNestedResultMapId());
                    value = getRowValue(rsw, resultMap, getColumnPrefix(columnPrefix, constructorMapping));
                } else {
                    final TypeHandler<?> typeHandler = constructorMapping.getTypeHandler();
                    value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(column, columnPrefix));
                }
            } catch (ResultMapException | SQLException e) {
                throw new ExecutorException("Could not process result for mapping: " + constructorMapping, e);
            }
            constructorArgTypes.add(parameterType);
            constructorArgs.add(value);
            foundValues = value != null || foundValues;
        }
        return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
    }

    private Object createByConstructorSignature(ResultSetWrapper rsw, Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) throws SQLException {
        final Constructor<?>[] constructors = resultType.getDeclaredConstructors();
        final Constructor<?> defaultConstructor = findDefaultConstructor(constructors);
        if (defaultConstructor != null) {
            return createUsingConstructor(rsw, resultType, constructorArgTypes, constructorArgs, defaultConstructor);
        } else {
            for (Constructor<?> constructor : constructors) {
                if (allowedConstructorUsingTypeHandlers(constructor, rsw.getJdbcTypes())) {
                    return createUsingConstructor(rsw, resultType, constructorArgTypes, constructorArgs, constructor);
                }
            }
        }
        throw new ExecutorException("No constructor found in " + resultType.getName() + " matching " + rsw.getClassNames());
    }

    private Object createUsingConstructor(ResultSetWrapper rsw, Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, Constructor<?> constructor) throws SQLException {
        boolean foundValues = false;
        for (int i = 0; i < constructor.getParameterTypes().length; i++) {
            Class<?> parameterType = constructor.getParameterTypes()[i];
            String columnName = rsw.getColumnNames().get(i);
            TypeHandler<?> typeHandler = rsw.getTypeHandler(parameterType, columnName);
            Object value = typeHandler.getResult(rsw.getResultSet(), columnName);
            constructorArgTypes.add(parameterType);
            constructorArgs.add(value);
            foundValues = value != null || foundValues;
        }
        return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
    }

    private Constructor<?> findDefaultConstructor(final Constructor<?>[] constructors) {
        if (constructors.length == 1) {
            return constructors[0];
        }

        for (final Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(AutomapConstructor.class)) {
                return constructor;
            }
        }
        return null;
    }
    private boolean allowedConstructorUsingTypeHandlers(final Constructor<?> constructor, final List<JdbcType> jdbcTypes) {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length != jdbcTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!typeHandlerRegistry.hasTypeHandler(parameterTypes[i], jdbcTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String getColumnPrefix(String parentPrefix, ResultMapping resultMapping) {
        final StringBuilder columnPrefixBuilder = new StringBuilder();
        if (parentPrefix != null) {
            columnPrefixBuilder.append(parentPrefix);
        }
        if (resultMapping.getColumnPrefix() != null) {
            columnPrefixBuilder.append(resultMapping.getColumnPrefix());
        }
        return columnPrefixBuilder.length() == 0 ? null : columnPrefixBuilder.toString().toUpperCase(Locale.ENGLISH);
    }


}

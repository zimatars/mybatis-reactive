package com.waterdrop.mybatisreactive.executor.keygen;

import com.waterdrop.mybatisreactive.exception.ReactiveMybatisException;
import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.resultset.ResultSetProxyHandler;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;


public class R2dbcKeyGenerator implements ReactiveKeyGenerator {

    public static final R2dbcKeyGenerator INSTANCE = new R2dbcKeyGenerator();


    @Override
    public void processBefore(ReactiveExecutor executor, MappedStatement ms, Statement stmt, Object parameter) {
        stmt.returnGeneratedValues(ms.getKeyProperties());
    }

    @Override
    public Mono<Void> processAfter(ReactiveExecutor executor, MappedStatement ms, Result result, Object parameter) {
        final String[] keyProperties = ms.getKeyProperties();
        if (keyProperties == null || keyProperties.length == 0) {
            return Mono.empty();
        }
        TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
        ResultSetProxyHandler resultSetProxyHandler = new ResultSetProxyHandler();
        ResultSet proxyResultSet = (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class}, resultSetProxyHandler);
        return Mono.from(result.map((row, rowMetadata) -> {
            MetaObject metaParam = ms.getConfiguration().newMetaObject(parameter);
            for (String keyProperty : keyProperties) {
                if (!metaParam.hasSetter(keyProperty)) {
                    throw new ExecutorException("No setter found for the keyProperty '" + keyProperty + "' in '"
                            + metaParam.getOriginalObject().getClass().getName() + "'.");
                }
                Class<?> propertyType = metaParam.getSetterType(keyProperty);
                //                Object value = row.get(keyProperty, propertyType);
                TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(propertyType, JdbcType.OTHER);
                if (typeHandler == null) {
                    // Error?
                } else {
                    resultSetProxyHandler.initRowInfo(row, rowMetadata);
                    Object value = null;
                    try {
                        value = typeHandler.getResult(proxyResultSet, keyProperty);
                    } catch (SQLException exception) {
                        //shouldn't happen Exception
                        throw new ReactiveMybatisException(exception);
                    }
                    metaParam.setValue(keyProperty, value);
                }
            }
            return 0;
        })).then();
    }
}

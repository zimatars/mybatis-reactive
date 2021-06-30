/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.waterdrop.mybatisreactive.pageplugin;

import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import com.waterdrop.mybatisreactive.executor.statement.ReactiveStatementHandler;
import com.waterdrop.mybatisreactive.scripting.defaults.DefaultReactiveParameterHandler;
import com.waterdrop.mybatisreactive.toolkit.PluginUtils;
import com.waterdrop.mybatisreactive.toolkit.StringUtils;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.RowBounds;
import reactor.core.publisher.Mono;

import java.util.Properties;

/**
 * <p>
 * 分页拦截器
 * </p>
 *
 */
@Intercepts({@Signature(type = ReactiveStatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class PaginationInterceptor implements Interceptor {
    /**
     * 日志
     */
    private static final Log logger = LogFactory.getLog(PaginationInterceptor.class);

    /**
     * 溢出总页数，设置第一页
     */
    private boolean overflowCurrent = false;
    /**
     * 方言类型
     */
    private String dialectType;
    /**
     * 方言实现类
     */
    private String dialectClazz;

    /**
     * Physical Pagination Interceptor for all the queries with parameter {@link RowBounds}
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ReactiveStatementHandler statementHandler = (ReactiveStatementHandler) PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 先判断是不是SELECT操作
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }
        RowBounds rowBounds = (RowBounds) metaObject.getValue("delegate.rowBounds");
        /* 不需要分页的场合 */
        if (rowBounds == null || rowBounds == RowBounds.DEFAULT) {
            // 无需分页
            return invocation.proceed();
        }
        // 针对定义了rowBounds，做为mapper接口方法的参数
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();
        Connection connection = (Connection) invocation.getArgs()[0];
        DBType dbType = StringUtils.isNotEmpty(dialectType) ? DBType.getDBType(dialectType) : DBType.getDBType(connection.getMetadata().getDatabaseProductName().split(" ")[0]);
        String pageSql;
        if(logger.isDebugEnabled()){
            logger.debug("dbType:"+dbType + " paging");
        }
        if (rowBounds instanceof Pagination) {
            Pagination page = (Pagination) rowBounds;
            pageSql = DialectFactory.buildPaginationSql(page, originalSql, dbType, dialectClazz);
            if (page.isSearchCount()) {
                String countSql = getOriginalCountSql(originalSql);
                if(logger.isDebugEnabled()){
                    logger.debug("count sql:" + countSql);
                }
                Mono<Long> countMono = this.queryTotal(overflowCurrent, countSql, mappedStatement, boundSql, page, connection);
                return countMono.flatMap(it -> {
                    if (page.getTotal() > 0) {
                        setPageSql(metaObject, pageSql);
                    }
                    try {
                        return (Mono<?>) invocation.proceed();
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
            }
        } else {
            // support physical Pagination for RowBounds
            pageSql = DialectFactory.buildPaginationSql(rowBounds, originalSql, dbType, dialectClazz);
            if(logger.isDebugEnabled()){
                logger.debug("page sql:" + pageSql);
            }
        }
        setPageSql(metaObject, pageSql);
        return invocation.proceed();
    }

    private void setPageSql(MetaObject metaObject,String pageSql) {
        metaObject.setValue("delegate.boundSql.sql", pageSql);
        /*
         * <p> 禁用内存分页 </p>
         */
        metaObject.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
        metaObject.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
    }


    /**
     * <p>
     * 获取 COUNT 原生 SQL 包装
     * </p>
     *
     * @param originalSql
     * @return
     */
    public static String getOriginalCountSql(String originalSql) {
        return String.format("SELECT COUNT(1) FROM ( %s ) TOTAL", originalSql);
    }

    /**
     * 查询总记录条数
     *  @param sql
     * @param mappedStatement
     * @param boundSql
     * @param page
     * @return
     */
    protected Mono<Long> queryTotal(boolean overflowCurrent, String sql, MappedStatement mappedStatement, BoundSql boundSql, Pagination page, Connection connection) {
            Statement statement = connection.createStatement(sql);
            ReactiveParameterHandler parameterHandler = new DefaultReactiveParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            parameterHandler.setParameters(statement);
            return Mono.from(statement.execute())
                    .flatMap(result -> Mono.from(result.map((row, rowMetadata) -> row.get(0, long.class))))
                    .doOnNext(total->{
                        page.setTotal(total);
                        long pages = page.getPages();
                        //溢出总页数，设置第一页
                        if (overflowCurrent && (page.getCurrent() > pages)) {
                            page.setCurrent(1);
                        }
                    });
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ReactiveStatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties prop) {
        String dialectType = prop.getProperty("dialectType");
        String dialectClazz = prop.getProperty("dialectClazz");

        if (StringUtils.isNotEmpty(dialectType)) {
            this.dialectType = dialectType;
        }
        if (StringUtils.isNotEmpty(dialectClazz)) {
            this.dialectClazz = dialectClazz;
        }
    }

    public PaginationInterceptor setDialectType(String dialectType) {
        this.dialectType = dialectType;
        return this;
    }

    public PaginationInterceptor setDialectClazz(String dialectClazz) {
        this.dialectClazz = dialectClazz;
        return this;
    }

    public PaginationInterceptor setOverflowCurrent(boolean overflowCurrent) {
        this.overflowCurrent = overflowCurrent;
        return this;
    }

}

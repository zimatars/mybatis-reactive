package com.waterdrop.mybatisreactive.session;

import com.waterdrop.mybatisreactive.binding.ReactiveMapperRegistry;
import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.SimpleReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import com.waterdrop.mybatisreactive.executor.resultset.DefaultReactiveResultSetHandler;
import com.waterdrop.mybatisreactive.executor.resultset.ReactiveResultSetHandler;
import com.waterdrop.mybatisreactive.executor.statement.ReactiveStatementHandler;
import com.waterdrop.mybatisreactive.executor.statement.RoutingStatementHandler;
import com.waterdrop.mybatisreactive.mapping.ReactiveEnvironment;
import com.waterdrop.mybatisreactive.scripting.defaults.DefaultReactiveParameterHandler;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import com.waterdrop.mybatisreactive.transaction.r2dbc.R2dbcTransactionFactory;
import io.r2dbc.pool.ConnectionPool;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public class ReactiveConfiguration extends Configuration {
    protected ReactiveEnvironment reactiveEnvironment;

    protected final ReactiveMapperRegistry mapperRegistry = new ReactiveMapperRegistry(this);

    public ReactiveConfiguration(ReactiveEnvironment reactiveEnvironment) {
        this();
        this.reactiveEnvironment = reactiveEnvironment;
    }

    public ReactiveConfiguration() {
        super();
        typeAliasRegistry.registerAlias("R2DBC", R2dbcTransactionFactory.class);
        typeAliasRegistry.registerAlias("R2DBC_POOLED", ConnectionPool.class);
    }

    public ReactiveEnvironment getReactiveEnvironment() {
        return reactiveEnvironment;
    }

    public void setReactiveEnvironment(ReactiveEnvironment reactiveEnvironment) {
        this.reactiveEnvironment = reactiveEnvironment;
    }

    public ReactiveExecutor newReactiveExecutor(ReactiveTransaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
        executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
        ReactiveExecutor executor;
        if (ExecutorType.BATCH == executorType) {
//            executor = new BatchExecutor(this, transaction);
            throw new RuntimeException("todo");
        } else if (ExecutorType.REUSE == executorType) {
//            executor = new ReuseExecutor(this, transaction);
            throw new RuntimeException("todo");
        } else {
            executor = new SimpleReactiveExecutor(this, transaction);
        }
        if (cacheEnabled) {
//            executor = new CachingExecutor(executor);
        }
        executor = (ReactiveExecutor) interceptorChain.pluginAll(executor);
        return executor;
    }

    public <T> T getMapper(Class<T> type, ReactiveSqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    @Override
    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }

    public ReactiveMapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }


    public ReactiveStatementHandler newReactiveStatementHandler(ReactiveExecutor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        ReactiveStatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
        statementHandler = (ReactiveStatementHandler) interceptorChain.pluginAll(statementHandler);
        return statementHandler;
    }

    public ReactiveParameterHandler newReactiveParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        ReactiveParameterHandler parameterHandler = createReactiveParameterHandler(mappedStatement, parameterObject, boundSql);
        parameterHandler = (ReactiveParameterHandler) interceptorChain.pluginAll(parameterHandler);
        return parameterHandler;
    }

    public ReactiveResultSetHandler newReactiveResultSetHandler(ReactiveExecutor executor, MappedStatement mappedStatement, RowBounds rowBounds, ReactiveParameterHandler parameterHandler,
                                                        ResultHandler resultHandler, BoundSql boundSql) {
        ReactiveResultSetHandler resultSetHandler = new DefaultReactiveResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
        resultSetHandler = (ReactiveResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
        return resultSetHandler;
    }

    private ReactiveParameterHandler createReactiveParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        return new DefaultReactiveParameterHandler(mappedStatement, parameterObject, boundSql);
    }
}

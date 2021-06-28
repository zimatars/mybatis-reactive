/**
 * Copyright 2009-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.waterdrop.mybatisreactive.executor;

import com.waterdrop.mybatisreactive.executor.statement.ReactiveStatementHandler;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SimpleReactiveExecutor extends BaseReactiveExecutor {

    public SimpleReactiveExecutor(Configuration configuration, ReactiveTransaction transaction) {
        super(configuration, transaction);
    }

    @Override
    public Mono<Integer> doUpdate(MappedStatement ms, Object parameter) throws SQLException {
        Mono<Statement> stmt;
        ReactiveConfiguration configuration = (ReactiveConfiguration) ms.getConfiguration();
        ReactiveStatementHandler handler = configuration.newReactiveStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
        stmt = prepareStatement(handler, ms.getStatementLog());
        return stmt.flatMap(handler::update);
    }

    @Override
    public <E> Flux<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Mono<Statement> stmt;
        ReactiveConfiguration configuration = (ReactiveConfiguration) ms.getConfiguration();
        ReactiveStatementHandler handler = configuration.newReactiveStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        stmt = prepareStatement(handler, ms.getStatementLog());
        return stmt.flatMapMany(it->handler.query(it, resultHandler));
    }

    @Override
    protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    /*Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    Cursor<E> cursor = handler.queryCursor(stmt);
    stmt.closeOnCompletion();
    return cursor;*/
        //todo
        return null;
    }

    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) {
        return Collections.emptyList();
    }

    private Mono<Statement> prepareStatement(ReactiveStatementHandler handler, Log statementLog) throws SQLException {
        Mono<Connection> connection = getConnection(statementLog);
        return transaction.getTimeout().defaultIfEmpty(0).flatMap(timeout->
                connection.flatMap(c -> handler.prepare(c, timeout)).doOnNext(handler::parameterize)
        );
//        stmt = handler.prepare(connection, transaction.getTimeout());
//    handler.parameterize(stmt);
//        return stmt;
    }

}

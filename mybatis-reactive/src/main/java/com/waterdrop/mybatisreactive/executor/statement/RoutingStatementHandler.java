/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.waterdrop.mybatisreactive.executor.statement;

import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Clinton Begin
 */
public class RoutingStatementHandler implements ReactiveStatementHandler {

  private final ReactiveStatementHandler delegate;

  public RoutingStatementHandler(ReactiveExecutor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

    switch (ms.getStatementType()) {
      //todo STATEMENT,CALLABLE StatementHandler
      /*case STATEMENT:
        delegate = new SimpleReactiveStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;*/
      case PREPARED:
        delegate = new PreparedReactiveStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      /*case CALLABLE:
        delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;*/
      default:
        throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }

  }

  @Override
  public Mono<Statement> prepare(Connection connection, Integer transactionTimeout) {
    return delegate.prepare(connection, transactionTimeout);
  }

  @Override
  public void parameterize(Statement statement) {
    delegate.parameterize(statement);
  }


  @Override
  public Mono<Integer> update(Statement statement) {
    return delegate.update(statement);
  }

  @Override
  public <E> Flux<E> query(Statement statement, ResultHandler resultHandler) {
    return delegate.query(statement, resultHandler);
  }

  @Override
  public BoundSql getBoundSql() {
    return delegate.getBoundSql();
  }

  @Override
  public ReactiveParameterHandler getParameterHandler() {
    return delegate.getParameterHandler();
  }
}

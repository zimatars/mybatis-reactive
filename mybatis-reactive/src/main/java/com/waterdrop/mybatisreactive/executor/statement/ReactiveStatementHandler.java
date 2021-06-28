/**
 *    Copyright 2009-2016 the original author or authors.
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

import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public interface ReactiveStatementHandler {

  Mono<Statement> prepare(Connection connection, Integer transactionTimeout);

  void parameterize(Statement statement);

  Mono<Integer> update(Statement statement);

  <E> Flux<E> query(Statement statement, ResultHandler resultHandler);

  BoundSql getBoundSql();

  ReactiveParameterHandler getParameterHandler();

}

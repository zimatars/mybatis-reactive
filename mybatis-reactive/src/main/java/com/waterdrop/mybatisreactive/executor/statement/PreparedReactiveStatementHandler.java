/**
 *    Copyright 2009-2018 the original author or authors.
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
import com.waterdrop.mybatisreactive.executor.keygen.ReactiveKeyGenerator;
import com.waterdrop.mybatisreactive.executor.resultset.RowWrap;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class PreparedReactiveStatementHandler extends BaseReactiveStatementHandler {

  public PreparedReactiveStatementHandler(ReactiveExecutor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  @Override
  public Mono<Integer> update(Statement statement) {
    ReactiveKeyGenerator keyGenerator = ReactiveKeyGenerator.convertFromKeyGenerator(mappedStatement.getKeyGenerator());
    Object parameterObject = boundSql.getParameterObject();
    return Mono.from(statement.execute()).flatMap(result->{
      return keyGenerator.processAfter(executor, mappedStatement, result, parameterObject)
              .then(Mono.from(result.getRowsUpdated()));
    });
  }


  @Override
  public <E> Flux<E> query(Statement statement, ResultHandler resultHandler) {
    return Flux.from(statement.execute()).map(it -> (Result) it).collectList().flatMapMany(results -> resultSetHandler.handleResultSets(results));
  }


  @Override
  protected Statement instantiateStatement(Connection connection) {
    String sql = boundSql.getSql();
    if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
      ReactiveKeyGenerator keyGenerator = ReactiveKeyGenerator.convertFromKeyGenerator(mappedStatement.getKeyGenerator());
      Statement statement = connection.createStatement(sql);
      keyGenerator.processBefore(executor, mappedStatement, statement, null);
      return statement;
    } else if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
      return connection.createStatement(sql);
    } else {
      return connection.createStatement(sql);
    }
  }

  @Override
  public void parameterize(Statement statement) {
    parameterHandler.setParameters(statement);
  }

}

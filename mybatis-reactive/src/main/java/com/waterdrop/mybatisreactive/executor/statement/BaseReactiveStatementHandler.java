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

import com.waterdrop.mybatisreactive.exception.ReactiveMybatisException;
import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import com.waterdrop.mybatisreactive.executor.resultset.ReactiveResultSetHandler;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public abstract class BaseReactiveStatementHandler implements ReactiveStatementHandler {

  protected final Configuration configuration;
  protected final ObjectFactory objectFactory;
  protected final TypeHandlerRegistry typeHandlerRegistry;
  protected final ReactiveResultSetHandler resultSetHandler;
  protected final ReactiveParameterHandler parameterHandler;

  protected final ReactiveExecutor executor;
  protected final MappedStatement mappedStatement;
  protected final RowBounds rowBounds;

  protected BoundSql boundSql;

  protected BaseReactiveStatementHandler(ReactiveExecutor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    if (boundSql == null) { // issue #435, get the key before calculating the statement
      //todo generateKeys
//      generateKeys(parameterObject);
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    this.boundSql = boundSql;
    if(!(configuration instanceof ReactiveConfiguration)){
      throw new RuntimeException("configuration must be ReactiveConfiguration.class");
    }
    ReactiveConfiguration reactiveConfiguration = (ReactiveConfiguration) this.configuration;
    this.parameterHandler = reactiveConfiguration.newReactiveParameterHandler(mappedStatement, parameterObject, boundSql);
    this.resultSetHandler = reactiveConfiguration.newReactiveResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
  }

  @Override
  public BoundSql getBoundSql() {
    return boundSql;
  }

  @Override
  public ReactiveParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  @Override
  public Mono<Statement> prepare(Connection connection, Integer transactionTimeout) {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
      statement = instantiateStatement(connection);
      setStatementTimeout(statement, transactionTimeout);
      setFetchSize(statement);
      return Mono.just(statement);
    } catch (SQLException e) {
      throw new ReactiveMybatisException(e);
    } catch (Exception e) {
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

  protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
    /*Integer queryTimeout = null;
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (configuration.getDefaultStatementTimeout() != null) {
      queryTimeout = configuration.getDefaultStatementTimeout();
    }
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);*/
  }

  protected void setFetchSize(Statement stmt) throws SQLException {
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize != null) {
      stmt.fetchSize(fetchSize);
      return;
    }
    Integer defaultFetchSize = configuration.getDefaultFetchSize();
    if (defaultFetchSize != null) {
      stmt.fetchSize(defaultFetchSize);
    }
  }

  /*protected void generateKeys(Object parameter) {
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    ErrorContext.instance().store();
    keyGenerator.processBefore(executor, mappedStatement, null, parameter);
    ErrorContext.instance().recall();
  }*/

}

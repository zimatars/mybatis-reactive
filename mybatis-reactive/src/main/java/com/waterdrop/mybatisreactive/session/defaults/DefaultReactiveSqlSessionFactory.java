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
package com.waterdrop.mybatisreactive.session.defaults;

import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.mapping.ReactiveEnvironment;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSession;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransactionFactory;
import com.waterdrop.mybatisreactive.transaction.r2dbc.R2dbcTransactionFactory;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class DefaultReactiveSqlSessionFactory implements ReactiveSqlSessionFactory {

  private final ReactiveConfiguration configuration;

  public DefaultReactiveSqlSessionFactory(ReactiveConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public ReactiveSqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }

  @Override
  public ReactiveSqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }

  @Override
  public ReactiveSqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

  @Override
  public ReactiveSqlSession openSession(TransactionIsolationLevel level) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
  }

  @Override
  public ReactiveSqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return openSessionFromDataSource(execType, level, false);
  }

  @Override
  public ReactiveSqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return openSessionFromDataSource(execType, null, autoCommit);
  }

  @Override
  public ReactiveSqlSession openSession(Connection connection) {
    return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
  }

  @Override
  public ReactiveSqlSession openSession(ExecutorType execType, Connection connection) {
    return openSessionFromConnection(execType, connection);
  }

  @Override
  public ReactiveConfiguration getConfiguration() {
    return configuration;
  }

  private ReactiveSqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    ReactiveTransaction tx = null;
    try {
      Environment environment1 = configuration.getEnvironment();
      final ReactiveEnvironment environment = configuration.getReactiveEnvironment();
      final ReactiveTransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getConnectionFactory(), level, autoCommit);
      final ReactiveExecutor executor = configuration.newReactiveExecutor(tx, execType);
      return new DefaultReactiveSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
//      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  private ReactiveSqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
    try {
      boolean autoCommit;
      try {
        autoCommit = connection.getAutoCommit();
      } catch (SQLException e) {
        // Failover to true, as most poor drivers
        // or databases won't support transactions
        autoCommit = true;
      }
//      final Environment environment = configuration.getEnvironment();
//      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
//      final Transaction tx = transactionFactory.newTransaction(connection);
      final ReactiveExecutor executor = configuration.newReactiveExecutor(null, execType);
      return new DefaultReactiveSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  private ReactiveTransactionFactory getTransactionFactoryFromEnvironment(ReactiveEnvironment environment) {
    if (environment == null || environment.getTransactionFactory() == null) {
      return new R2dbcTransactionFactory();
    }
    return environment.getTransactionFactory();
  }

  private void closeTransaction(Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      } catch (SQLException ignore) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

}

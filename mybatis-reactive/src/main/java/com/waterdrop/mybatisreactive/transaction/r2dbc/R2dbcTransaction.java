/**
 *    Copyright 2009-2020 the original author or authors.
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
package com.waterdrop.mybatisreactive.transaction.r2dbc;

import com.waterdrop.mybatisreactive.exception.ReactiveMybatisException;
import com.waterdrop.mybatisreactive.toolkit.TransactionConvert;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

/**
 * {@link Transaction} that makes use of the JDBC commit and rollback facilities directly.
 * It relies on the connection retrieved from the dataSource to manage the scope of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores commit or rollback requests when autocommit is on.
 *
 * @author Clinton Begin
 *
 * @see R2dbcTransactionFactory
 */
public class R2dbcTransaction implements ReactiveTransaction {

  private static final Log log = LogFactory.getLog(R2dbcTransaction.class);

  protected Mono<Connection> connection;
  protected ConnectionFactory connectionFactory;
  protected TransactionIsolationLevel level;
  protected boolean autoCommit;

  public R2dbcTransaction(ConnectionFactory cf, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    connectionFactory = cf;
    level = desiredLevel;
    autoCommit = desiredAutoCommit;
  }

  public R2dbcTransaction(Connection connection) {
    this.connection = Mono.just(connection);
  }

  public R2dbcTransaction(Mono<Connection> connection) {
    this.connection = connection;
  }

  @Override
  public Mono<Connection> getConnection() throws SQLException {
    if (connection == null) {
      connection = openConnection();
    }
    return connection;
  }

  @Override
  public Mono<Void> commit() {
    if(connection == null){
      throw new ReactiveMybatisException("connection is null when committing");
    }
    return connection.flatMap(c -> {
      if (!c.isAutoCommit()) {
        if (log.isDebugEnabled()) {
          log.debug("Committing JDBC Connection [" + connection + "]");
        }
        return Mono.from(c.commitTransaction());
      } else {
        return Mono.empty();
      }
    });

  }

  @Override
  public Mono<Void> rollback() throws SQLException {
    if (connection != null) {
      return connection.flatMap(c -> {
        if (!c.isAutoCommit()) {
          if (log.isDebugEnabled()) {
            log.debug("Rolling back JDBC Connection [" + connection + "]");
          }
          return Mono.from(c.rollbackTransaction());
        } else {
          return Mono.empty();
        }
      });
    }else {
      return Mono.empty();
    }
  }

  @Override
  public Mono<Void> close() throws SQLException {
    if (connection != null) {
      resetAutoCommit();
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + connection + "]");
      }
      return connection.flatMap(c -> Mono.from(c.close()));
    }else {
      throw new ReactiveMybatisException("connection is null when closing");
    }
  }

  protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
    connection = connection.doOnNext(c->{
      if (c.isAutoCommit() != desiredAutoCommit) {
        if (log.isDebugEnabled()) {
          log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
        }
        c.setAutoCommit(desiredAutoCommit);
      }
    });

  }

  protected void resetAutoCommit() {
    connection = connection.doOnNext(c->{
      if (!c.isAutoCommit()) {
        // MyBatis does not call commit/rollback on a connection if just selects were performed.
        // Some databases start transactions with select statements
        // and they mandate a commit/rollback before closing the connection.
        // A workaround is setting the autocommit to true before closing the connection.
        // Sybase throws an exception here.
        if (log.isDebugEnabled()) {
          log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
        }
        c.setAutoCommit(true);
      }
    });
  }

  protected Mono<Connection> openConnection() {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    connection = Mono.from(connectionFactory.create());
    if (level != null) {
      connection = connection.doOnNext(c->c.setTransactionIsolationLevel(TransactionConvert.TransactionIsolationLevelConvertIsolationLevel(level)));
    }
    setDesiredAutoCommit(autoCommit);
    return connection;
  }

  @Override
  public Mono<Integer> getTimeout() {
    return Mono.empty();
  }

}

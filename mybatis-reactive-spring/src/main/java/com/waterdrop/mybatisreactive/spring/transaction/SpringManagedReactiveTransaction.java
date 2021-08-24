/**
 * Copyright 2010-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.waterdrop.mybatisreactive.spring.transaction;

import com.waterdrop.mybatisreactive.exception.ReactiveMybatisException;
import com.waterdrop.mybatisreactive.spring.logging.Logger;
import com.waterdrop.mybatisreactive.spring.logging.LoggerFactory;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.r2dbc.connection.ConnectionHolder;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.ResourceHolderSupport;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.util.Assert.notNull;

/**
 * {@code SpringManagedTransaction} handles the lifecycle of a JDBC connection. It retrieves a connection from Spring's
 * transaction manager and returns it back to it when it is no longer needed.
 * <p>
 * If Spring's transaction handling is active it will no-op all commit/rollback/close calls assuming that the Spring
 * transaction manager will do the job.
 * <p>
 * If it is not it will behave like {@code JdbcTransaction}.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
public class SpringManagedReactiveTransaction implements ReactiveTransaction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringManagedReactiveTransaction.class);

  private final ConnectionFactory connectionFactory;

  private Mono<Connection> connection;

  private AtomicReference<Connection> connectionRef = new AtomicReference<>();

  private Mono<Boolean> isConnectionTransactional;

  private Mono<Boolean> autoCommit;

  public SpringManagedReactiveTransaction(ConnectionFactory connectionFactory) {
    notNull(connectionFactory, "No connectionFactory specified");
    this.connectionFactory = connectionFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Connection> getConnection() {
    if (this.connection == null) {
     connection = openConnection();
    }
    return this.connection.doOnNext(c-> connectionRef.updateAndGet(prev->c));
  }

  /**
   * Gets a connection from Spring transaction manager and discovers if this {@code Transaction} should manage
   * connection or let it to Spring.
   * <p>
   * It also reads autocommit setting because when using Spring Transaction MyBatis thinks that autocommit is always
   * false and will always call commit/rollback so we need to no-op that calls.
   * @return
   */
  private Mono<Connection> openConnection() {
    this.connection = ConnectionFactoryUtils.getConnection(connectionFactory);
    this.autoCommit = this.connection.map(Connection::isAutoCommit);
    this.isConnectionTransactional = this.connection.flatMap(this::isConnectionTransactional);

    LOGGER.debug(() -> "R2DBC Connection [" + this.connection + "] will" + "be managed by Spring");
    return this.connection;
  }

  private void validConnectionRef(){
    if(connectionRef.get()==null){
      throw new ReactiveMybatisException("method getConnection() must be invoked before this operation");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> commit() {
    if(connection == null){
      throw new ReactiveMybatisException("connection is null when committing");
    }
    validConnectionRef();
    Connection c = connectionRef.get();
    if (!c.isAutoCommit()) {
      LOGGER.debug(() -> "Committing JDBC Connection [" + this.connection + "]");
      return Mono.from(c.commitTransaction());
    } else {
      return Mono.empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> rollback() {
    if (connection != null) {
      validConnectionRef();
      Connection c = connectionRef.get();
      if (!c.isAutoCommit()) {
        LOGGER.debug(()->"Rolling back R2DBC Connection [" + connection + "]");
        return Mono.from(c.rollbackTransaction());
      } else {
        return Mono.empty();
      }
    }else {
      return Mono.empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> close() {
    validConnectionRef();
    return ConnectionFactoryUtils.releaseConnection(connectionRef.get(), this.connectionFactory);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> getTimeout() {
    return getConnectionHolder().filter(ResourceHolderSupport::hasTimeout).map(ConnectionHolder::getTimeToLiveInSeconds);
  }

  private Mono<ConnectionHolder> getConnectionHolder(){
    return TransactionSynchronizationManager.forCurrentTransaction()
            .filter(TransactionSynchronizationManager::isSynchronizationActive)
            .flatMap((synchronizationManager) -> Mono.justOrEmpty((ConnectionHolder) synchronizationManager.getResource(connectionFactory)))
            .onErrorResume(NoTransactionException.class,e->Mono.empty());
  }

  public Mono<Boolean> isConnectionTransactional(Connection con) {
    return getConnectionHolder().map(it -> connectionEquals(it, con)).defaultIfEmpty(false);
  }

  private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
    Connection heldCon = conHolder.getConnection();
    // Explicitly check for identity too: for Connection handles that do not implement
    // "equals" properly, such as the ones Commons DBCP exposes).
    return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
            ConnectionFactoryUtils.getTargetConnection(heldCon).equals(passedInCon));
  }

}

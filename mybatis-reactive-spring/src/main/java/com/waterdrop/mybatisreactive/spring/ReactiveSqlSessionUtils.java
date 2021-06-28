/**
 * Copyright 2010-2020 the original author or authors.
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
package com.waterdrop.mybatisreactive.spring;

import com.waterdrop.mybatisreactive.mapping.ReactiveEnvironment;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSession;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory;
import com.waterdrop.mybatisreactive.spring.logging.Logger;
import com.waterdrop.mybatisreactive.spring.logging.LoggerFactory;
import com.waterdrop.mybatisreactive.spring.transaction.SpringManagedReactiveTransactionFactory;
import org.apache.ibatis.session.ExecutorType;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.reactive.ReactiveResourceSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

import static org.springframework.util.Assert.notNull;

/**
 * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from Spring
 * {@code TransactionSynchronizationManager}. Also works if no transaction is active.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
public final class ReactiveSqlSessionUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveSqlSessionUtils.class);

  private static final String NO_EXECUTOR_TYPE_SPECIFIED = "No ExecutorType specified";
  private static final String NO_SQL_SESSION_FACTORY_SPECIFIED = "No SqlSessionFactory specified";
  private static final String NO_SQL_SESSION_SPECIFIED = "No SqlSession specified";

  /**
   * This class can't be instantiated, exposes static utility methods only.
   */
  private ReactiveSqlSessionUtils() {
    // do nothing
  }

  /**
   * Creates a new MyBatis {@code SqlSession} from the {@code SqlSessionFactory} provided as a parameter and using its
   * {@code DataSource} and {@code ExecutorType}
   *
   * @param sessionFactory
   *          a MyBatis {@code SqlSessionFactory} to create new sessions
   * @return a MyBatis {@code SqlSession}
   * @throws TransientDataAccessResourceException
   *           if a transaction is active and the {@code SqlSessionFactory} is not using a
   *           {@code SpringManagedTransactionFactory}
   */
  public static Mono<ReactiveSqlSession> getSqlSession(ReactiveSqlSessionFactory sessionFactory) {
    ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
    return getSqlSession(sessionFactory, executorType, null);
  }

  /**
   * Gets an SqlSession from Spring Transaction Manager or creates a new one if needed. Tries to get a SqlSession out of
   * current transaction. If there is not any, it creates a new one. Then, it synchronizes the SqlSession with the
   * transaction if Spring TX is active and <code>SpringManagedTransactionFactory</code> is configured as a transaction
   * manager.
   *
   * @param sessionFactory
   *          a MyBatis {@code SqlSessionFactory} to create new sessions
   * @param executorType
   *          The executor type of the SqlSession to create
   * @param exceptionTranslator
   *          Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
   * @return an SqlSession managed by Spring Transaction Manager
   * @throws TransientDataAccessResourceException
   *           if a transaction is active and the {@code SqlSessionFactory} is not using a
   *           {@code SpringManagedTransactionFactory}
   * @see SpringManagedReactiveTransactionFactory
   */
  public static Mono<ReactiveSqlSession> getSqlSession(ReactiveSqlSessionFactory sessionFactory, ExecutorType executorType,
                                                       PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    notNull(executorType, NO_EXECUTOR_TYPE_SPECIFIED);

    return TransactionSynchronizationManager.forCurrentTransaction().map((synchronizationManager) -> {
      ReactiveSqlSessionHolder holder = (ReactiveSqlSessionHolder) synchronizationManager.getResource(sessionFactory);
      if (holder != null && holder.isSynchronizedWithTransaction()) {
        return sessionHolder(executorType, holder);
      } else {
        ReactiveSqlSession session = sessionFactory.openSession(executorType);
        registerSessionHolder(synchronizationManager, sessionFactory, executorType, exceptionTranslator, session);
        return session;
      }
    }).onErrorReturn(NoTransactionException.class,sessionFactory.openSession(executorType));
  }

  /**
   * Register session holder if synchronization is active (i.e. a Spring TX is active).
   *
   * Note: The DataSource used by the Environment should be synchronized with the transaction either through
   * DataSourceTxMgr or another tx synchronization. Further assume that if an exception is thrown, whatever started the
   * transaction will handle closing / rolling back the Connection associated with the SqlSession.
   *
   * @param sessionFactory
   *          sqlSessionFactory used for registration.
   * @param executorType
   *          executorType used for registration.
   * @param exceptionTranslator
   *          persistenceExceptionTranslator used for registration.
   * @param session
   *          sqlSession used for registration.
   */
  private static void registerSessionHolder(TransactionSynchronizationManager transactionSynchronizationManager,ReactiveSqlSessionFactory sessionFactory, ExecutorType executorType,
                                            PersistenceExceptionTranslator exceptionTranslator, ReactiveSqlSession session) {
    ReactiveSqlSessionHolder holder;
    if (transactionSynchronizationManager.isSynchronizationActive()) {
      ReactiveEnvironment environment = ((ReactiveConfiguration)sessionFactory.getConfiguration()).getReactiveEnvironment();

      if (environment.getTransactionFactory() instanceof SpringManagedReactiveTransactionFactory) {
        LOGGER.debug(() -> "Registering transaction synchronization for SqlSession [" + session + "]");

        holder = new ReactiveSqlSessionHolder(session, executorType, exceptionTranslator);
        transactionSynchronizationManager.bindResource(sessionFactory, holder);
        transactionSynchronizationManager
            .registerSynchronization(new ReactiveSqlSessionSynchronization(holder, sessionFactory,transactionSynchronizationManager));
        holder.setSynchronizedWithTransaction(true);
        holder.requested();
      } else {
        if (transactionSynchronizationManager.getResource(environment.getConnectionFactory()) == null) {
          LOGGER.debug(() -> "SqlSession [" + session
              + "] was not registered for synchronization because DataSource is not transactional");
        } else {
          throw new TransientDataAccessResourceException(
              "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
        }
      }
    } else {
      LOGGER.debug(() -> "SqlSession [" + session
          + "] was not registered for synchronization because synchronization is not active");
    }

  }

  private static ReactiveSqlSession sessionHolder(ExecutorType executorType, ReactiveSqlSessionHolder holder) {
      if (holder.getExecutorType() != executorType) {
        throw new TransientDataAccessResourceException(
            "Cannot change the ExecutorType when there is an existing transaction");
      }

      holder.requested();

      LOGGER.debug(() -> "Fetched SqlSession [" + holder.getSqlSession() + "] from current transaction");
    return holder.getSqlSession();
  }

  /**
   * Checks if {@code SqlSession} passed as an argument is managed by Spring {@code TransactionSynchronizationManager}
   * If it is not, it closes it, otherwise it just updates the reference counter and lets Spring call the close callback
   * when the managed transaction ends
   *  @param session
   *          a target SqlSession
   * @param sessionFactory
   * @return
   */
  public static Mono<Void> closeSqlSession(ReactiveSqlSession session, ReactiveSqlSessionFactory sessionFactory) {
    notNull(session, NO_SQL_SESSION_SPECIFIED);
    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    return TransactionSynchronizationManager.forCurrentTransaction().flatMap((synchronizationManager) -> {
      ReactiveSqlSessionHolder holder = (ReactiveSqlSessionHolder) synchronizationManager.getResource(sessionFactory);
      if ((holder != null) && (holder.getSqlSession() == session)) {
        LOGGER.debug(() -> "Releasing transactional SqlSession [" + session + "]");
        holder.released();
        return Mono.empty();
      } else {
        LOGGER.debug(() -> "Closing non transactional SqlSession [" + session + "]");
        return session.close();
      }
    }).onErrorResume(NoTransactionException.class,e->session.close());
  }

  /**
   * Returns if the {@code SqlSession} passed as an argument is being managed by Spring
   *
   * @param session
   *          a MyBatis SqlSession to check
   * @param sessionFactory
   *          the SqlSessionFactory which the SqlSession was built with
   * @return true if session is transactional, otherwise false
   */
  public static Mono<Boolean> isSqlSessionTransactional(ReactiveSqlSession session, ReactiveSqlSessionFactory sessionFactory) {
    notNull(session, NO_SQL_SESSION_SPECIFIED);
    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    return TransactionSynchronizationManager.forCurrentTransaction().map((synchronizationManager) -> {
      ReactiveSqlSessionHolder holder = (ReactiveSqlSessionHolder) synchronizationManager.getResource(sessionFactory);
      return holder != null && holder.getSqlSession() == session;
    }).onErrorReturn(NoTransactionException.class, Boolean.FALSE);
  }

  private static final class ReactiveSqlSessionSynchronization extends ReactiveResourceSynchronization<ReactiveSqlSessionHolder,ReactiveSqlSessionFactory> {
    private final ReactiveSqlSessionHolder holder;
    private final ReactiveSqlSessionFactory sessionFactory;
    public ReactiveSqlSessionSynchronization(ReactiveSqlSessionHolder holder, ReactiveSqlSessionFactory sessionFactory, TransactionSynchronizationManager synchronizationManager) {
      super(holder, sessionFactory, synchronizationManager);
      this.holder = holder;
      this.sessionFactory = sessionFactory;
    }

    @Override
    protected boolean shouldUnbindAtCompletion() {
      return !this.holder.isOpen();
    }

    @Override
    protected boolean shouldReleaseBeforeCompletion() {
      return super.shouldReleaseBeforeCompletion();
    }

    @Override
    protected boolean shouldReleaseAfterCompletion(ReactiveSqlSessionHolder resourceHolder) {
      return super.shouldReleaseAfterCompletion(resourceHolder);
    }

    @Override
    protected Mono<Void> processResourceAfterCommit(ReactiveSqlSessionHolder resourceHolder) {
      return super.processResourceAfterCommit(resourceHolder);
    }

    @Override
    protected Mono<Void> releaseResource(ReactiveSqlSessionHolder resourceHolder, ReactiveSqlSessionFactory resourceKey) {
      return resourceHolder.getSqlSession().close();
    }

    @Override
    protected Mono<Void> cleanupResource(ReactiveSqlSessionHolder resourceHolder, ReactiveSqlSessionFactory resourceKey, boolean committed) {
      resourceHolder.reset();
      return Mono.empty();
    }
  }

}

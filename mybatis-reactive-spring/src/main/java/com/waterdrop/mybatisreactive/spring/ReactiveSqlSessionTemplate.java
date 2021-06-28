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

import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSession;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory;
import io.r2dbc.spi.Connection;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.ibatis.reflection.ExceptionUtil.unwrapThrowable;
import static com.waterdrop.mybatisreactive.spring.ReactiveSqlSessionUtils.*;
import static org.springframework.util.Assert.notNull;

/**
 * Thread safe, Spring managed, {@code SqlSession} that works with Spring transaction management to ensure that that the
 * actual SqlSession used is the one associated with the current Spring transaction. In addition, it manages the session
 * life-cycle, including closing, committing or rolling back the session as necessary based on the Spring transaction
 * configuration.
 * <p>
 * The template needs a SqlSessionFactory to create SqlSessions, passed as a constructor argument. It also can be
 * constructed indicating the executor type to be used, if not, the default executor type, defined in the session
 * factory will be used.
 * <p>
 * This template converts MyBatis PersistenceExceptions into unchecked DataAccessExceptions, using, by default, a
 * {@code MyBatisExceptionTranslator}.
 * <p>
 * Because SqlSessionTemplate is thread safe, a single instance can be shared by all DAOs; there should also be a small
 * memory savings by doing this. This pattern can be used in Spring configuration files as follows:
 *
 * <pre class="code">
 * {@code
 * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
 *   <constructor-arg ref="sqlSessionFactory" />
 * </bean>
 * }
 * </pre>
 *
 * @author Putthiphong Boonphong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see SqlSessionFactory
 * @see MyBatisExceptionTranslator
 */
public class ReactiveSqlSessionTemplate implements ReactiveSqlSession, DisposableBean {

  private final ReactiveSqlSessionFactory sqlSessionFactory;

  private final ExecutorType executorType;

  private final ReactiveSqlSession sqlSessionProxy;

  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * Constructs a Spring managed SqlSession with the {@code SqlSessionFactory} provided as an argument.
   *
   * @param sqlSessionFactory
   *          a factory of SqlSession
   */
  public ReactiveSqlSessionTemplate(ReactiveSqlSessionFactory sqlSessionFactory) {
    this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
  }

  /**
   * Constructs a Spring managed SqlSession with the {@code SqlSessionFactory} provided as an argument and the given
   * {@code ExecutorType} {@code ExecutorType} cannot be changed once the {@code SqlSessionTemplate} is constructed.
   *
   * @param sqlSessionFactory
   *          a factory of SqlSession
   * @param executorType
   *          an executor type on session
   */
  public ReactiveSqlSessionTemplate(ReactiveSqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
    this(sqlSessionFactory, executorType,
        new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getReactiveEnvironment().getConnectionFactory(), true));
  }

  /**
   * Constructs a Spring managed {@code SqlSession} with the given {@code SqlSessionFactory} and {@code ExecutorType}. A
   * custom {@code SQLExceptionTranslator} can be provided as an argument so any {@code PersistenceException} thrown by
   * MyBatis can be custom translated to a {@code RuntimeException} The {@code SQLExceptionTranslator} can also be null
   * and thus no exception translation will be done and MyBatis exceptions will be thrown
   *
   * @param sqlSessionFactory
   *          a factory of SqlSession
   * @param executorType
   *          an executor type on session
   * @param exceptionTranslator
   *          a translator of exception
   */
  public ReactiveSqlSessionTemplate(ReactiveSqlSessionFactory sqlSessionFactory, ExecutorType executorType,
                                    PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
    notNull(executorType, "Property 'executorType' is required");

    this.sqlSessionFactory = sqlSessionFactory;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
    this.sqlSessionProxy = (ReactiveSqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
        new Class[] { ReactiveSqlSession.class }, new ReactiveSqlSessionInterceptor());
  }

  public ReactiveSqlSessionFactory getSqlSessionFactory() {
    return this.sqlSessionFactory;
  }

  public ExecutorType getExecutorType() {
    return this.executorType;
  }

  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return this.exceptionTranslator;
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public <T> Mono<T> selectOne(String statement) {
    return this.sqlSessionProxy.selectOne(statement);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public <T> Mono<T> selectOne(String statement, Object parameter) {
    return this.sqlSessionProxy.selectOne(statement, parameter);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public <E> Flux<E> selectList(String statement) {
    return this.sqlSessionProxy.selectList(statement);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public <E> Flux<E> selectList(String statement, Object parameter) {
    return this.sqlSessionProxy.selectList(statement, parameter);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public <E> Flux<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return this.sqlSessionProxy.selectList(statement, parameter, rowBounds);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> insert(String statement) {
    return this.sqlSessionProxy.insert(statement);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> insert(String statement, Object parameter) {
    return this.sqlSessionProxy.insert(statement, parameter);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> update(String statement) {
    return this.sqlSessionProxy.update(statement);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> update(String statement, Object parameter) {
    return this.sqlSessionProxy.update(statement, parameter);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> delete(String statement) {
    return this.sqlSessionProxy.delete(statement);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Integer> delete(String statement, Object parameter) {
    return this.sqlSessionProxy.delete(statement, parameter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getMapper(Class<T> type) {
    return ((ReactiveConfiguration)getConfiguration()).getMapper(type, this);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Void> commit() {
    throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed ReactiveSqlSession");
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Void> commit(boolean force) {
    throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed ReactiveSqlSession");
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Void> rollback() {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed ReactiveSqlSession");
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Void> rollback(boolean force) {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed ReactiveSqlSession");
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Void> close() {
    throw new UnsupportedOperationException("Manual close is not allowed over a Spring managed ReactiveSqlSession");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearCache() {
    this.sqlSessionProxy.clearCache();
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  public Configuration getConfiguration() {
    return this.sqlSessionFactory.getConfiguration();
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Mono<Connection> getConnection() {
    return this.sqlSessionProxy.getConnection();
  }

  /**
   * Allow gently dispose bean:
   *
   * <pre>
   * {@code
   *
   * <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
   *  <constructor-arg index="0" ref="sqlSessionFactory" />
   * </bean>
   * }
   * </pre>
   *
   * The implementation of {@link DisposableBean} forces spring context to use {@link DisposableBean#destroy()} method
   * instead of {@link ReactiveSqlSessionTemplate#close()} to shutdown gently.
   *
   * @see ReactiveSqlSessionTemplate#close()
   * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#inferDestroyMethodIfNecessary(Object, RootBeanDefinition)"
   * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#CLOSE_METHOD_NAME"
   */
  @Override
  public void destroy() throws Exception {
    // This method forces spring disposer to avoid call of SqlSessionTemplate.close() which gives
    // UnsupportedOperationException
  }

  /**
   * Proxy needed to route MyBatis method calls to the proper SqlSession got from Spring's Transaction Manager It also
   * unwraps exceptions thrown by {@code Method#invoke(Object, Object...)} to pass a {@code PersistenceException} to the
   * {@code PersistenceExceptionTranslator}.
   */
  private class ReactiveSqlSessionInterceptor implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Mono<ReactiveSqlSession> sqlSessionMono = getSqlSession(ReactiveSqlSessionTemplate.this.sqlSessionFactory,
          ReactiveSqlSessionTemplate.this.executorType, ReactiveSqlSessionTemplate.this.exceptionTranslator);
      if(method.getReturnType().equals(Flux.class)){
        return sqlSessionMono.flatMapMany(sqlSession -> {
          return invokeSqlSession(sqlSession, method, args);
        });
      }else {
        return sqlSessionMono.flatMap(sqlSession -> {
          return (Mono) invokeSqlSession(sqlSession, method, args);
        });
      }
    }

    private Publisher<?> invokeSqlSession(ReactiveSqlSession sqlSession,Method method, Object[] args){
      try {
        Object result = method.invoke(sqlSession, args);
        Mono<Boolean> isTransactionalMono = isSqlSessionTransactional(sqlSession, ReactiveSqlSessionTemplate.this.sqlSessionFactory);
        BiFunction<ReactiveSqlSession,Boolean,Mono<Void>> commitFunc = (session,isTransactional) -> {
                    if (!isTransactional) {
                      // force commit even on non-dirty sessions because some databases require
                      // a commit/rollback before calling close()
                      return session.commit(true);
                    } else {
                      return Mono.empty();
                    }
        };

        ReactiveSqlSession finalSqlSession = sqlSession;

        Mono<Void> closeMono = closeSqlSession(sqlSession, ReactiveSqlSessionTemplate.this.sqlSessionFactory);
        if(result instanceof Mono){
//          Function<Throwable,Mono> caFunc = (Throwable t)-> catchFunc(t, finalSqlSession);
          return isTransactionalMono.flatMap(isTransactional -> {
            return ((Mono) result).doFinally(it -> commitFunc.apply(finalSqlSession,isTransactional).subscribe()).doFinally(it -> closeMono.subscribe());
          });
        }
        if(result instanceof Flux){
          return isTransactionalMono.flatMapMany(isTransactional -> {
            return ((Flux) result).doFinally(it -> commitFunc.apply(finalSqlSession,isTransactional).subscribe()).doFinally(it -> closeMono.subscribe());
          });

        }
        return (Publisher) result;
      } catch (Throwable t) {
        Throwable unwrapped = unwrapThrowable(t);
        if (ReactiveSqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
          // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
          Mono<Void> closeMono = closeSqlSession(sqlSession, ReactiveSqlSessionTemplate.this.sqlSessionFactory);
          sqlSession = null;
          Throwable translated = ReactiveSqlSessionTemplate.this.exceptionTranslator
                  .translateExceptionIfPossible((PersistenceException) unwrapped);
          if (translated != null) {
            unwrapped = translated;
          }
          return closeMono.then(Mono.error(unwrapped));
        }else {
          return Mono.error(unwrapped);
        }
      }
    }

    private Mono<Object> catchFunc(Throwable t, ReactiveSqlSession sqlSession){
      Throwable unwrapped = unwrapThrowable(t);
      if (ReactiveSqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
        // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
        Mono<Void> closeMono = closeSqlSession(sqlSession, ReactiveSqlSessionTemplate.this.sqlSessionFactory);
        sqlSession = null;
        Throwable translated = ReactiveSqlSessionTemplate.this.exceptionTranslator
                .translateExceptionIfPossible((PersistenceException) unwrapped);
        if (translated != null) {
          unwrapped = translated;
        }
        return closeMono.then(Mono.error(unwrapped));
      }else {
        return Mono.error(unwrapped);
      }
    }
  }


}

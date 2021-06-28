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
package com.waterdrop.mybatisreactive.session.defaults;

import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSession;
import io.r2dbc.spi.Connection;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The default implementation for {@link SqlSession}.
 * Note that this class is not Thread-Safe.
 *
 * @author Clinton Begin
 */
public class DefaultReactiveSqlSession implements ReactiveSqlSession {

  private final ReactiveConfiguration configuration;
  private final ReactiveExecutor executor;

  private final boolean autoCommit;
  private boolean dirty;
  private List<Cursor<?>> cursorList;

  public DefaultReactiveSqlSession(ReactiveConfiguration configuration, ReactiveExecutor executor, boolean autoCommit) {
    this.configuration = configuration;
    this.executor = executor;
    this.dirty = false;
    this.autoCommit = autoCommit;
  }

  public DefaultReactiveSqlSession(ReactiveConfiguration configuration, ReactiveExecutor executor) {
    this(configuration, executor, false);
  }

  @Override
  public <T> Mono<T> selectOne(String statement) {
    return this.selectOne(statement, null);
  }

  @Override
  public <T> Mono<T> selectOne(String statement, Object parameter) {
    // Popular vote was to return null on 0 results and throw exception on too many.
    Flux<T> result = this.selectList(statement, parameter);
    return result.collectList().flatMap(list -> {
      if (list.size() == 1) {
        return Mono.just(list.get(0));
      } else if (list.size() > 1) {
        return Mono.error(new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size()));
      } else {
        return Mono.empty();
      }
    });

  }

  @Override
  public <E> Flux<E> selectList(String statement) {
    return this.selectList(statement, null);
  }

  @Override
  public <E> Flux<E> selectList(String statement, Object parameter) {
    return this.selectList(statement, parameter, RowBounds.DEFAULT);
  }

  @Override
  public <E> Flux<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return selectList(statement, parameter, rowBounds, Executor.NO_RESULT_HANDLER);
  }

  private <E> Flux<E> selectList(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  @Override
  public Mono<Integer> insert(String statement) {
    return insert(statement, null);
  }

  @Override
  public Mono<Integer> insert(String statement, Object parameter) {
    return update(statement, parameter);
  }

  @Override
  public Mono<Integer> update(String statement) {
    return update(statement, null);
  }

  @Override
  public Mono<Integer> update(String statement, Object parameter) {
    try {
      dirty = true;
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.update(ms, wrapCollection(parameter));
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  @Override
  public Mono<Integer> delete(String statement) {
    return update(statement, null);
  }

  @Override
  public Mono<Integer> delete(String statement, Object parameter) {
    return update(statement, parameter);
  }

  @Override
  public Mono<Void> commit() {
    return Mono.empty();
  }

  @Override
  public Mono<Void> commit(boolean force) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> rollback() {
    return null;
  }

  @Override
  public Mono<Void> rollback(boolean force) {
    return null;
  }

  @Override
  public Mono<Void> close() {
    try {
      executor.close(isCommitOrRollbackRequired(false));
      closeCursors();
      dirty = false;
    } finally {
      ErrorContext.instance().reset();
    }
    return Mono.empty();
  }

  private void closeCursors() {
    if (cursorList != null && !cursorList.isEmpty()) {
      for (Cursor<?> cursor : cursorList) {
        try {
          cursor.close();
        } catch (IOException e) {
          throw ExceptionFactory.wrapException("Error closing cursor.  Cause: " + e, e);
        }
      }
      cursorList.clear();
    }
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return configuration.getMapper(type, this);
  }

  @Override
  public Mono<Connection> getConnection() {
    try {
      return executor.getTransaction().getConnection();
    } catch (SQLException e) {
      throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
    }
  }

  @Override
  public void clearCache() {
    executor.clearLocalCache();
  }

  private <T> void registerCursor(Cursor<T> cursor) {
    if (cursorList == null) {
      cursorList = new ArrayList<>();
    }
    cursorList.add(cursor);
  }

  private boolean isCommitOrRollbackRequired(boolean force) {
    return (!autoCommit && dirty) || force;
  }

  private Object wrapCollection(final Object object) {
    return ParamNameResolver.wrapToMapIfCollection(object, null);
  }

  /**
   * @deprecated Since 3.5.5
   */
  @Deprecated
  public static class StrictMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -5741767162221585340L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + this.keySet());
      }
      return super.get(key);
    }

  }

}

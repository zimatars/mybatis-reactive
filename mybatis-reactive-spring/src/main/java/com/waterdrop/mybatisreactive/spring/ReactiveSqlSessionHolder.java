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
package com.waterdrop.mybatisreactive.spring;

import com.waterdrop.mybatisreactive.session.ReactiveSqlSession;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.support.ResourceHolderSupport;

import static org.springframework.util.Assert.notNull;

/**
 * Used to keep current {@code ReactiveSqlSession} in {@code TransactionSynchronizationManager}. The {@code ReactiveSqlSessionFactory}
 * that created that {@code ReactiveSqlSession} is used as a key. {@code ExecutorType} is also kept to be able to check if the
 * user is trying to change it during a TX (that is not allowed) and throw a Exception in that case.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
public final class ReactiveSqlSessionHolder extends ResourceHolderSupport {

  private final ReactiveSqlSession sqlSession;

  private final ExecutorType executorType;

  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * Creates a new holder instance.
   *
   * @param sqlSession
   *          the {@code SqlSession} has to be hold.
   * @param executorType
   *          the {@code ExecutorType} has to be hold.
   * @param exceptionTranslator
   *          the {@code PersistenceExceptionTranslator} has to be hold.
   */
  public ReactiveSqlSessionHolder(ReactiveSqlSession sqlSession, ExecutorType executorType,
                                  PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sqlSession, "SqlSession must not be null");
    notNull(executorType, "ExecutorType must not be null");

    this.sqlSession = sqlSession;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
  }

  public ReactiveSqlSession getSqlSession() {
    return sqlSession;
  }

  public ExecutorType getExecutorType() {
    return executorType;
  }

  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return exceptionTranslator;
  }

}

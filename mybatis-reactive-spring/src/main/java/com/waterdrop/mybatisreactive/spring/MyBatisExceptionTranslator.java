/*
 * Copyright 2010-2021 the original author or authors.
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

import io.r2dbc.spi.ConnectionFactory;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import javax.sql.DataSource;

/**
 * Default exception translator.
 *
 * Translates MyBatis SqlSession returned exception into a Spring {@code DataAccessException} using Spring's
 * {@code SQLExceptionTranslator} Can load {@code SQLExceptionTranslator} eagerly or when the first exception is
 * translated.
 *
 * @author Eduardo Macarron
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  public MyBatisExceptionTranslator(ConnectionFactory connectionFactory, boolean exceptionTranslatorLazyInit) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }
      //todo impl sql exceptionTranslator

      return new MyBatisSystemException(e);
    }
    return null;
  }

}

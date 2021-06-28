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

import com.waterdrop.mybatisreactive.transaction.ReactiveTransaction;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransactionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import java.util.Properties;

/**
 * Creates a {@code SpringManagedTransaction}.
 *
 * @author Hunter Presnall
 */
public class SpringManagedReactiveTransactionFactory implements ReactiveTransactionFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties(Properties props) {
    // not needed in this version
  }

  @Override
  public ReactiveTransaction newTransaction(Connection conn) {
    throw new UnsupportedOperationException("New Spring transactions require a DataSource");
  }

  @Override
  public ReactiveTransaction newTransaction(ConnectionFactory connectionFactory, TransactionIsolationLevel level, boolean autoCommit) {
    return new SpringManagedReactiveTransaction(connectionFactory);
  }


}

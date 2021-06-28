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
package com.waterdrop.mybatisreactive.transaction;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import java.util.Properties;

/**
 * Creates {@link ReactiveTransaction} instances.
 *
 * @author Clinton Begin
 */
public interface ReactiveTransactionFactory {

  /**
   * Sets transaction factory custom properties.
   * @param props
   *          the new properties
   */
  default void setProperties(Properties props) {
    // NOP
  }

  /**
   * Creates a {@link ReactiveTransaction} out of an existing connection.
   * @param conn Existing database connection
   * @return Transaction
   * @since 3.1.0
   */
  ReactiveTransaction newTransaction(Connection conn);

  /**
   * Creates a {@link ReactiveTransaction} out of a datasource.
   * @param connectionFactory ConnectionFactory to take the connection from
   * @param level Desired isolation level
   * @param autoCommit Desired autocommit
   * @return Transaction
   * @since 3.1.0
   */
  ReactiveTransaction newTransaction(ConnectionFactory connectionFactory, TransactionIsolationLevel level, boolean autoCommit);

}

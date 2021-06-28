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
import reactor.core.publisher.Mono;

import java.sql.SQLException;

/**
 * Wraps a database connection.
 * Handles the connection lifecycle that comprises: its creation, preparation, commit/rollback and close.
 *
 * @author Clinton Begin
 */
public interface ReactiveTransaction {

  /**
   * Retrieve inner database connection.
   * @return DataBase connection
   * @throws SQLException
   *           the SQL exception
   */
  Mono<Connection> getConnection() throws SQLException;

  /**
   * Commit inner database connection.
   * @throws SQLException
   *           the SQL exception
   * @return
   */
  Mono<Void> commit();

  /**
   * Rollback inner database connection.
   * @throws SQLException
   *           the SQL exception
   */
  Mono<Void> rollback() throws SQLException;

  /**
   * Close inner database connection.
   * @throws SQLException
   *           the SQL exception
   */
  Mono<Void> close() throws SQLException;

  /**
   * Get transaction timeout if set.
   *
   * @return the timeout
   * @throws SQLException
   *           the SQL exception
   */
  Mono<Integer> getTimeout();

}

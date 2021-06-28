/**
 *    Copyright 2009-2019 the original author or authors.
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
package com.waterdrop.mybatisreactive.mapping;

import com.waterdrop.mybatisreactive.transaction.ReactiveTransactionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;

/**
 * @author Clinton Begin
 */
public final class ReactiveEnvironment {
  private final String id;
  private final ReactiveTransactionFactory transactionFactory;
  private final ConnectionFactory connectionFactory;

  public ReactiveEnvironment(String id, ReactiveTransactionFactory transactionFactory, ConnectionFactory connectionFactory) {
    if (id == null) {
      throw new IllegalArgumentException("Parameter 'id' must not be null");
    }
    if (transactionFactory == null) {
      throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
    }
    this.id = id;
    if (connectionFactory == null) {
      throw new IllegalArgumentException("Parameter 'connectionFactory' must not be null");
    }
    this.transactionFactory = transactionFactory;
    this.connectionFactory = connectionFactory;
  }

  public static class Builder {
    private final String id;
    private ReactiveTransactionFactory transactionFactory;
    private ConnectionFactory connectionFactory;

    public Builder(String id) {
      this.id = id;
    }

    public Builder transactionFactory(ReactiveTransactionFactory transactionFactory) {
      this.transactionFactory = transactionFactory;
      return this;
    }

    public Builder connectionFactory(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
      return this;
    }

    public String id() {
      return this.id;
    }

    public ReactiveEnvironment build() {
      return new ReactiveEnvironment(this.id, this.transactionFactory, this.connectionFactory);
    }

  }

  public String getId() {
    return this.id;
  }

  public ReactiveTransactionFactory getTransactionFactory() {
    return this.transactionFactory;
  }

  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }
}

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
package com.waterdrop.mybatisreactive.scripting.defaults;

import com.waterdrop.mybatisreactive.executor.parameter.ReactiveParameterHandler;
import com.waterdrop.mybatisreactive.executor.parameter.PreparedStatementProxy;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class DefaultReactiveParameterHandler implements ReactiveParameterHandler {
  private final DefaultParameterHandler delegate;
  private final Configuration configuration;

  public DefaultReactiveParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    this.delegate = new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
    this.configuration = mappedStatement.getConfiguration();
  }

  @Override
  public Object getParameterObject() {
    return delegate.getParameterObject();
  }

  @Override
  public void setParameters(Statement statement) {
    PreparedStatement ps = (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class[]{PreparedStatement.class}, new PreparedStatementProxy(statement,configuration));
    delegate.setParameters(ps);
  }

}

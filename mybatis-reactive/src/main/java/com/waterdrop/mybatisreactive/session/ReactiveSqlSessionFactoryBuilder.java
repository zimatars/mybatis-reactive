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
package com.waterdrop.mybatisreactive.session;

import com.waterdrop.mybatisreactive.builder.xml.ReactiveXMLConfigBuilder;
import com.waterdrop.mybatisreactive.session.defaults.DefaultReactiveSqlSessionFactory;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

/**
 * Builds {@link SqlSession} instances.
 *
 * @author Clinton Begin
 */
public class ReactiveSqlSessionFactoryBuilder {

  public ReactiveSqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  public ReactiveSqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }

  public ReactiveSqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

  public ReactiveSqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      ReactiveXMLConfigBuilder parser = new ReactiveXMLConfigBuilder(reader, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  public ReactiveSqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }

  public ReactiveSqlSessionFactory build(InputStream inputStream, String environment) {
    return build(inputStream, environment, null);
  }

  public ReactiveSqlSessionFactory build(InputStream inputStream, Properties properties) {
    return build(inputStream, null, properties);
  }

  public ReactiveSqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      ReactiveXMLConfigBuilder parser = new ReactiveXMLConfigBuilder(inputStream, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  public ReactiveSqlSessionFactory build(ReactiveConfiguration config) {
    return new DefaultReactiveSqlSessionFactory(config);
  }

}

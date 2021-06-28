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
package com.waterdrop.mybatisreactive.spring.sample.config;

import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory;
import com.waterdrop.mybatisreactive.spring.ReactiveSqlSessionFactoryBean;
import com.waterdrop.mybatisreactive.spring.ReactiveSqlSessionTemplate;
import com.waterdrop.mybatisreactive.spring.mapper.MapperFactoryBean;
import com.waterdrop.mybatisreactive.spring.sample.mapper.UserMapper;
import com.waterdrop.mybatisreactive.spring.sample.service.FooService;
import com.waterdrop.mybatisreactive.spring.transaction.SpringManagedReactiveTransactionFactory;
import com.waterdrop.mybatisreactive.transaction.ReactiveTransactionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class SampleConfig {

  @Bean
  public ConnectionFactory connectionFactory(){
    ConnectionFactory factory = ConnectionFactories.get("r2dbc:mysql://root:123456@127.0.0.1:3306/reactive");
    return factory;
  }

  @Bean
  public ReactiveTransactionManager reactiveTransactionManager() {
    return new R2dbcTransactionManager(connectionFactory());
  }

  @Bean
  public ReactiveTransactionFactory springManagedReactiveTransactionFactory() {
    return new SpringManagedReactiveTransactionFactory();
  }

  @Bean
  public ReactiveSqlSessionFactory sqlSessionFactory() throws Exception {
    ReactiveSqlSessionFactoryBean ss = new ReactiveSqlSessionFactoryBean();
    ss.setConnectionFactory(connectionFactory());
    ss.setMapperLocations(new ClassPathResource("mapper/UserMapper.xml"));
    ss.setTransactionFactory(springManagedReactiveTransactionFactory());
    ReactiveSqlSessionFactory sqlSessionFactory = ss.getObject();
    sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
    return sqlSessionFactory;
  }

  @Bean
  public UserMapper userMapper() throws Exception {
    // when using javaconfig a template requires less lines than a MapperFactoryBean
    ReactiveSqlSessionTemplate sqlSessionTemplate = new ReactiveSqlSessionTemplate(sqlSessionFactory());
    return sqlSessionTemplate.getMapper(UserMapper.class);
  }

  @Bean
  public UserMapper userMapperWithFactory() throws Exception {
    MapperFactoryBean<UserMapper> mapperFactoryBean = new MapperFactoryBean<>();
    mapperFactoryBean.setMapperInterface(UserMapper.class);
    mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory());
    mapperFactoryBean.afterPropertiesSet();
    return mapperFactoryBean.getObject();
  }

  @Bean
  public FooService fooService() throws Exception {
    return new FooService(userMapper());
  }

}

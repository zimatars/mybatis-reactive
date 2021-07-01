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
package com.waterdrop.mybatisreactive.pageplugin.sample.config;

import com.waterdrop.mybatisreactive.pageplugin.PaginationInterceptor;
import com.waterdrop.mybatisreactive.spring.annotation.MapperScan;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan("com.waterdrop.mybatisreactive.pageplugin.sample.*")
@ComponentScan("com.waterdrop.mybatisreactive.pageplugin.sample.*")
@SpringBootApplication
@EnableTransactionManagement
@Configuration
public class SampleConfig {

  /*@Bean
  public ConnectionFactory connectionFactory(){
    ConnectionFactory factory = ConnectionFactories.get("r2dbc:mysql://root:123456@127.0.0.1:3306/reactive");
    return factory;
  }*/

  @Bean
  public PaginationInterceptor paginationInterceptor(){
    return new PaginationInterceptor();
  }

}

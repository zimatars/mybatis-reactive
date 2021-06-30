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
/**
 * MyBatis @Configuration style sample
 */
package com.waterdrop.mybatisreactive.pageplugin;

import com.waterdrop.mybatisreactive.pageplugin.sample.config.SampleConfig;
import com.waterdrop.mybatisreactive.pageplugin.sample.domain.User;
import com.waterdrop.mybatisreactive.pageplugin.sample.service.FooService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

@SpringBootTest(classes = SampleConfig.class)
//@SpringJUnitConfig(classes = SampleConfig.class)
class SampleJavaConfigTest {

  @Autowired
  private FooService fooService;

  @Test
  void testFindById() {
    Mono<User> user = fooService.doSomeBusinessStuff(1L);
    StepVerifier.create(user)
            .expectNextCount(1)
            .verifyComplete();
  }

  @Test
  void testListAll() {
    Pagination pagination = new Pagination(1, 10);
    Flux<User> users = fooService.listPage(pagination);
    StepVerifier.create(users)
            .thenConsumeWhile(Objects::nonNull, System.out::println)
            .verifyComplete();
  }

  @Test
  void testInsertWithException() {
    Mono<Void> ret = fooService.testInsertWithException();
    StepVerifier.create(ret)
            .verifyComplete();
//            .verifyError(ArithmeticException.class);
  }

}

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
package com.waterdrop.mybatisreactive.boot.autoconfigure.sample.service;

import com.waterdrop.mybatisreactive.boot.autoconfigure.sample.domain.User;
import com.waterdrop.mybatisreactive.boot.autoconfigure.sample.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * FooService simply receives a userId and uses a mapper to get a record from the database.
 */
@Service
public class FooService {

  private final UserMapper userMapper;

  public FooService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public Mono<User> doSomeBusinessStuff(Long userId) {
    return this.userMapper.getById(userId);
  }

  public Flux<User> listAll(){
    return userMapper.selectList();
  }

  @Transactional
  public Mono<Void> testInsertWithException(){
    User user1 = new User();
    user1.setName("a1");
    user1.setAge(20);
    user1.setCreatedTime(LocalDateTime.now());
    Mono<Integer> ret = userMapper.insert(user1);
    //throw Runtime Exception
    return ret.map(e->e/0).then();
  }

}

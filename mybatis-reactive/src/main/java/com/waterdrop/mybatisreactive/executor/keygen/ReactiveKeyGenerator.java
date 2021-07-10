/**
 *    Copyright 2009-2015 the original author or authors.
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
package com.waterdrop.mybatisreactive.executor.keygen;

import com.waterdrop.mybatisreactive.executor.ReactiveExecutor;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import reactor.core.publisher.Mono;


/**
 * @author Clinton Begin
 */
public interface ReactiveKeyGenerator {

  static ReactiveKeyGenerator convertFromKeyGenerator(KeyGenerator keyGenerator){
    if(keyGenerator instanceof Jdbc3KeyGenerator){
      return R2dbcKeyGenerator.INSTANCE;
    }else {
      return ReactiveNoKeyGenerator.INSTANCE;
    }
  }

  void processBefore(ReactiveExecutor executor, MappedStatement ms, Statement stmt, Object parameter);

  Mono<Void> processAfter(ReactiveExecutor executor, MappedStatement ms, Result result, Object parameter);

}

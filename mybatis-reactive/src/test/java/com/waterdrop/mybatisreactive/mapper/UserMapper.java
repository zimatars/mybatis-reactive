package com.waterdrop.mybatisreactive.mapper;

import com.waterdrop.mybatisreactive.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * User Mapper
 *
 */
public interface UserMapper {

    Mono<User> getById(Long id);

    Flux<User> selectList();

    Mono<Integer> batchInsert(List<User> userList);

    Mono<Integer> insert(User user);

    Mono<Integer> updateById(User demo);

    Mono<Boolean> deleteById(Long id);

}

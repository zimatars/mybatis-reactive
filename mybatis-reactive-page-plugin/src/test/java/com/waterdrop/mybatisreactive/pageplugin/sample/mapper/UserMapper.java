package com.waterdrop.mybatisreactive.pageplugin.sample.mapper;

import com.waterdrop.mybatisreactive.pageplugin.Pagination;
import com.waterdrop.mybatisreactive.pageplugin.sample.domain.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * User Mapper
 *
 */
public interface UserMapper {

    Mono<User> getById(Long id);

    Flux<User> selectList(Pagination pagination);

    Mono<Integer> batchInsert(List<User> userList);

    Mono<Integer> insert(User user);

    Mono<Integer> updateById(User demo);

    Mono<Boolean> deleteById(Long id);

    Mono<Void> updateErrorSql();

}

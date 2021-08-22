package com.waterdrop.mybatisreactive.sample;

import com.waterdrop.mybatisreactive.builder.xml.ReactiveXMLConfigBuilder;
import com.waterdrop.mybatisreactive.sample.entity.User;
import com.waterdrop.mybatisreactive.sample.mapper.UserMapper;
import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory;
import com.waterdrop.mybatisreactive.session.defaults.DefaultReactiveSqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserMapperTest {
    private static ReactiveSqlSessionFactory reactiveSqlSessionFactory;


    @BeforeAll
    public static void setUp() {
        ReactiveXMLConfigBuilder xmlConfigBuilder = new ReactiveXMLConfigBuilder(UserMapperTest.class.getResourceAsStream("/mybatis-config.xml"));
        ReactiveConfiguration configuration = xmlConfigBuilder.parse();
        reactiveSqlSessionFactory = new DefaultReactiveSqlSessionFactory(configuration);
        reactiveSqlSessionFactory.openSession(true)
                .update("com.waterdrop.mybatisreactive.sample.mapper.UserMapper.ddl")
                .block();
    }

    private UserMapper getUserMapper(){
        return reactiveSqlSessionFactory.openSession().getMapper(UserMapper.class);
    }

    @Test
    public void testSelectList() {
        Flux<User> users = getUserMapper().selectList();
        StepVerifier.create(users)
                .thenConsumeWhile(Objects::nonNull, System.out::println)
                .verifyComplete();
    }

    @Test
    public void testGetById() {
        Mono<User> user = getUserMapper().getById(1L);
        StepVerifier.create(user)
                .thenConsumeWhile(Objects::nonNull, System.out::println)
                .verifyComplete();
    }

    @Test
    public void testInsert() {
        User user = new User();
        user.setName("张三");
        user.setAge(19);
        user.setCreatedTime(LocalDateTime.now());
        Mono<Integer> updateRows = getUserMapper().insert(user);
        StepVerifier.create(updateRows)
                .expectNextMatches(it-> it==1 && user.getId()!=null)
                .verifyComplete();
    }

    @Test
    public void testUpdateById() {
        User user = new User();
        user.setName("王五");
//        user.setAge(20);
        user.setCreatedTime(LocalDateTime.now());
        user.setId(1L);
        Mono<Integer> updateRows = getUserMapper().updateById(user);
        StepVerifier.create(updateRows)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    public void testDeleteById() {
        Mono<Boolean> updateRows = getUserMapper().deleteById(-1L);
        StepVerifier.create(updateRows)
                .expectNext(Boolean.FALSE)
                .verifyComplete();
    }

}

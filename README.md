# mybatis reactive
### 响应式mybatis框架

# Getting Started
## mybatis-reactive
```
        <dependency>
            <groupId>com.waterdrop</groupId>
            <artifactId>mybatis-reactive</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

## mybatis-reactive-spring
## 与spring集成
```
        <dependency>
            <groupId>com.waterdrop</groupId>
            <artifactId>mybatis-reactive-spring</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

## mybatis-reactive-boot-starer
## 与spring boot集成
### 1.starter
```
        <dependency>
            <groupId>com.waterdrop</groupId>
            <artifactId>mybatis-reactive-boot-starter</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
### 2.mapper声明
```
@Mapper
public interface UserMapper {
    Mono<User> getById(Long id);

    Flux<User> selectList();
}
```
## mybatis-reactive-page-plugin
## 分页插件
```
        <dependency>
            <groupId>com.waterdrop</groupId>
            <artifactId>mybatis-reactive-page-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
### 配置分页插件
```
  @Bean
  public PaginationInterceptor paginationInterceptor(){
    return new PaginationInterceptor();
  }
```
```
@Mapper
public interface UserMapper {
    Flux<User> selectList(Pagination pagination);
}
```
# todo ...

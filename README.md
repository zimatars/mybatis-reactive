# mybatis reactive
### 响应式、非阻塞 mybatis 实现, kotlin协程支持 

# Getting Started
## mybatis-reactive
```
    <dependency>
        <groupId>com.waterdrop</groupId>
        <artifactId>mybatis-reactive</artifactId>
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
### 2.数据源配置
```
spring:
  r2dbc:
    url: r2dbc:mysql://ip:port/databaseName
    username: name
    password: ******
```
### 3.mapper声明
```java
@Mapper
public interface UserMapper {
    Mono<User> getById(Long id);

    Flux<User> selectList();
}
```
kotlin协程支持，mapper支持suspend函数
```kotlin
@Mapper
interface UserKtMapper {
    suspend fun getById(id: Long?): User?

    suspend fun selectList(): List<User>
}
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

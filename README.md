# mybatis-reactive
### reactive,non-blocking io mybatis based on r2dbc with kotlin coroutines support
### 响应式、非阻塞 mybatis 实现, 支持kotlin协程

mybatis-reactive是一个响应式版mybatis持久层框架，可以通过简单的 XML 或注解来配置和映射原始类型、接口和 Java POJO为数据库中的记录。

支持以下特性
* **Reactive**

    mapper 返回值支持 Mono、Flux，以非阻塞方式与数据库IO请求

* **协程**
    
    支持kotlin协程，映射函数可声明为suspend，以同步方式处理异步

* **插件机制**

    支持插件机制，可实现类似分页插件等

### 与mybatis对比接口变化
|  mybatis-reactive   | mybatis (jdbc)  |
|  ----  | ----  |
| ReactiveSqlSession  | SqlSession |
| ReactiveSqlSessionFactory  | SqlSessionFactory |
| ReactiveExecutor  | Executor |
| ReactiveStatementHandler  | StatementHandler |
| ReactiveParameterHandler  | ParameterHandler |
| ReactiveResultSetHandler  | ResultSetHandler |

## 快速开始
### 下载源码编译
```sh
    git clone https://github.com/zimatars/mybatis-reactive.git
    cd mybatis-reactive/
    mvn -Dmaven.test.skip=true clean install
```

### 选择驱动
  可参考该网址 https://r2dbc.io/drivers 选择对应r2dbc驱动

  如：mysql
```xml
    <dependency>
        <groupId>dev.miku</groupId>
        <artifactId>r2dbc-mysql</artifactId>
        <version>0.8.2.RELEASE</version>
    </dependency>
```

### mybatis-reactive
```xml
    <dependency>
        <groupId>com.waterdrop</groupId>
        <artifactId>mybatis-reactive</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
```
### mybatis-reactive-boot-starer
### 与spring boot集成
#### 1.starter
```xml
    <dependency>
        <groupId>com.waterdrop</groupId>
        <artifactId>mybatis-reactive-boot-starter</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
```
#### 2.数据源配置
```yml
spring:
  r2dbc:
    url: r2dbc:mysql://ip:port/databaseName
    username: name
    password: 123***
```
#### 3.mapper声明
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
    suspend fun getById(id: Long): User?

    suspend fun selectList(): List<User>
}
```
### mybatis-reactive-spring
### 与spring集成
```xml
    <dependency>
        <groupId>com.waterdrop</groupId>
        <artifactId>mybatis-reactive-spring</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
```
### mybatis-reactive-page-plugin
### 分页插件
```xml
    <dependency>
        <groupId>com.waterdrop</groupId>
        <artifactId>mybatis-reactive-page-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
```
#### 配置分页插件
```java
  @Bean
  public PaginationInterceptor paginationInterceptor(){
    return new PaginationInterceptor();
  }
```
```java
@Mapper
public interface UserMapper {
    Flux<User> selectList(Pagination pagination);
}
```


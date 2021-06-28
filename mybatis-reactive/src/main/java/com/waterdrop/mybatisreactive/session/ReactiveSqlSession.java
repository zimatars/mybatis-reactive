package com.waterdrop.mybatisreactive.session;

import io.r2dbc.spi.Connection;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * The primary Java interface for working with MyBatis.
 * Through this interface you can execute commands, get mappers and manage transactions.
 * ReactiveSqlSession
 */
public interface ReactiveSqlSession {
    /**
     * Retrieve a single row mapped from the statement key.
     * @param <T> the returned object type
     * @param statement
     *          the statement
     * @return Mapped object
     */
    <T> Mono<T> selectOne(String statement);

    /**
     * Retrieve a single row mapped from the statement key and parameter.
     * @param <T> the returned object type
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @return Mapped object
     */
    <T> Mono<T> selectOne(String statement, Object parameter);

    /**
     * Retrieve a list of mapped objects from the statement key.
     * @param <E> the returned list element type
     * @param statement Unique identifier matching the statement to use.
     * @return List of mapped object
     */
    <E> Flux<E> selectList(String statement);

    /**
     * Retrieve a list of mapped objects from the statement key and parameter.
     * @param <E> the returned list element type
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @return List of mapped object
     */
    <E> Flux<E> selectList(String statement, Object parameter);

    /**
     * Retrieve a list of mapped objects from the statement key and parameter,
     * within the specified row bounds.
     * @param <E> the returned list element type
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param rowBounds  Bounds to limit object retrieval
     * @return List of mapped object
     */
    <E> Flux<E> selectList(String statement, Object parameter, RowBounds rowBounds);

    /**
     * Execute an insert statement.
     * @param statement Unique identifier matching the statement to execute.
     * @return int The number of rows affected by the insert.
     */
    Mono<Integer> insert(String statement);

    /**
     * Execute an insert statement with the given parameter object. Any generated
     * autoincrement values or selectKey entries will modify the given parameter
     * object properties. Only the number of rows affected will be returned.
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return int The number of rows affected by the insert.
     */
    Mono<Integer> insert(String statement, Object parameter);

    /**
     * Execute an update statement. The number of rows affected will be returned.
     * @param statement Unique identifier matching the statement to execute.
     * @return int The number of rows affected by the update.
     */
    Mono<Integer> update(String statement);

    /**
     * Execute an update statement. The number of rows affected will be returned.
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return int The number of rows affected by the update.
     */
    Mono<Integer> update(String statement, Object parameter);

    /**
     * Execute a delete statement. The number of rows affected will be returned.
     * @param statement Unique identifier matching the statement to execute.
     * @return int The number of rows affected by the delete.
     */
    Mono<Integer> delete(String statement);

    /**
     * Execute a delete statement. The number of rows affected will be returned.
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return int The number of rows affected by the delete.
     */
    Mono<Integer> delete(String statement, Object parameter);


    Mono<Void> commit();

    Mono<Void> commit(boolean force);

    Mono<Void> rollback();

    Mono<Void> rollback(boolean force);

    /**
     * Closes the session.
     */
    Mono<Void> close();

    /**
     * Clears local session cache.
     */
    void clearCache();

    /**
     * Retrieves current configuration.
     * @return Configuration
     */
    Configuration getConfiguration();

    /**
     * Retrieves a mapper.
     * @param <T> the mapper type
     * @param type Mapper interface class
     * @return a mapper bound to this SqlSession
     */
    <T> T getMapper(Class<T> type);

    /**
     * Retrieves inner database connection.
     * @return Connection
     */
    Mono<Connection> getConnection();
}

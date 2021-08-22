package com.waterdrop.mybatisreactive.executor.parameter;

import io.r2dbc.spi.Statement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class PreparedStatementProxy implements InvocationHandler {
    private Statement statement;
    private Configuration configuration;

    public PreparedStatementProxy(Statement statement,Configuration configuration) {
        this.statement = statement;
        this.configuration = configuration;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if(methodName.equals("setNull")){
            int parameterIndex = (int) args[0];
            JdbcType jdbcType = JdbcType.forCode((int) args[1]);
            TypeHandler<?> typeHandler = configuration.getTypeHandlerRegistry().getTypeHandler(jdbcType);
            Class<?> paramClazz = Object.class;
            if(typeHandler!=null){
                ParameterizedType parameterizedType = (ParameterizedType) typeHandler.getClass().getGenericSuperclass();
                paramClazz = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            }
            statement.bindNull(parameterIndex-1, paramClazz);
        }else if (method.getName().startsWith("set")){
            statement.bind((int)args[0]-1,args[1]);
        }else {
            throw new RuntimeException("not support invoke for poxy PreparedStatement:"+methodName);
        }
        return null;
    }
}

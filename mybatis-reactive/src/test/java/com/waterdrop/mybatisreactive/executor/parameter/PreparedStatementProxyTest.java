package com.waterdrop.mybatisreactive.executor.parameter;

import com.waterdrop.mybatisreactive.session.ReactiveConfiguration;
import io.r2dbc.spi.Statement;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreparedStatementProxyTest {

    @Test
    public void setParameters() throws SQLException {
        StatementProxyHandler statementProxyHandler = new StatementProxyHandler();
        Statement statement = (Statement) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Statement.class}, statementProxyHandler);
        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class[]{PreparedStatement.class}, new PreparedStatementProxy(statement, new ReactiveConfiguration()));

        ps.setNull(1, JdbcType.INTEGER.TYPE_CODE);
        assertEquals("bindNull", statementProxyHandler.getMethod().getName());
        assertEquals(0, statementProxyHandler.getArgs()[0]);
        assertEquals(Integer.class, statementProxyHandler.getArgs()[1]);
    }

    public static class StatementProxyHandler implements InvocationHandler {
        private Method method;
        private Object[] args;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            this.method = method;
            this.args = args;
            return null;
        }

        public Method getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }
    }
}

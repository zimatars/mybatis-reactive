package com.waterdrop.mybatisreactive.executor.resultset;

import io.r2dbc.spi.Result;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

public class StatementProxy implements InvocationHandler {

    private Iterator<List<RowWrap>> results;
    private Result result;

    public StatementProxy(Iterator<List<RowWrap>> results) {
        this.results = results;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(method.getName().equals("getResultSet")){
            return getResultSet();
        }else {
            throw new UnsupportedOperationException("reactive proxy Statement don't implement "+method.getName());
        }
    }

    public ResultSet getResultSet(){
//        result.map((row,rowMetadata)->row.get(""))
        if(results.hasNext()){
            List<RowWrap> rowWrapList = results.next();
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                    new Class[]{ResultSet.class}, new ResultSetProxy(rowWrapList));
        }else {
            return null;
        }
    }


}

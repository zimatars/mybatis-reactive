package com.waterdrop.mybatisreactive.executor.resultset;

import com.waterdrop.mybatisreactive.toolkit.Defaults;
import io.r2dbc.spi.RowMetadata;
import org.apache.ibatis.executor.ExecutorException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResultSetProxy implements InvocationHandler {
    private final List<RowWrap> rowWrapList;
    private final Iterator<RowWrap> rowWrapIterator;
    private RowWrap cur;
    private Object lastColumnValue;

    public ResultSetProxy(List<RowWrap> rowWrapList) {
        this.rowWrapList = rowWrapList;
        this.rowWrapIterator = rowWrapList.iterator();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("isClosed")) {
            return isClosed();
        } else if (methodName.equals("next")) {
            boolean hasNext = rowWrapIterator.hasNext();
            if (hasNext) {
                cur = rowWrapIterator.next();
                lastColumnValue = null;
            }
            return hasNext;
        } else if (methodName.equals("getMetaData")) {
            if (rowWrapList.isEmpty()) {
                return null;
            }
            RowMetadata rowMetadata = rowWrapList.get(0).getRowMetadata();
            return new ResultSetMetaDataSimulation(new ArrayList<>(rowMetadata.getColumnNames()));
        } else if (methodName.equals("getType")) {
            return getType();
        } else if (methodName.startsWith("get")) {
            Class<?> returnTypeClass = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length < 1) {
                throw new ExecutorException(methodName + " parameterTypes is empty");
            }
            Class<?> parameterType = parameterTypes[0];
            if (parameterType.isPrimitive()) {
                //by the index of the column
                lastColumnValue = cur.getRow().get((int) args[0] - 1, returnTypeClass);
            } else {
                //by the name of the column
                lastColumnValue = cur.getRow().get((String) args[0], returnTypeClass);
            }
            if (lastColumnValue == null && returnTypeClass.isPrimitive()) {
                return Defaults.defaultValue(returnTypeClass);
            }
            return lastColumnValue;
        } else if (methodName.startsWith("wasNull")) {
            return wasNull();
        } else if (methodName.startsWith("close")) {
            return null;
        } else {
            throw new UnsupportedOperationException("reactive proxy ResultSet don't implement " + method.getName());
        }
    }

    boolean isClosed() {
        return false;
    }

    boolean wasNull() {
        return lastColumnValue == null;
    }

    int getType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

}

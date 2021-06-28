package com.waterdrop.mybatisreactive.executor.resultset;

import com.waterdrop.mybatisreactive.toolkit.Defaults;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.apache.ibatis.executor.ExecutorException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class ResultSetProxyHandler implements InvocationHandler {
    private Row row;
    private RowMetadata rowMetadata;
    private Object lastColumnValue;

    public void initRowInfo(Row row,RowMetadata rowMetadata){
        this.row = row;
        this.rowMetadata = rowMetadata;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("isClosed")) {
            return isClosed();
        } else if (methodName.equals("next")) {
            return true;
        } else if (methodName.equals("getMetaData")) {
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
            Function<Object,Object> convertFunc=null;
            Class<?> askDbType = returnTypeClass;
            //adapter type
            if(returnTypeClass.equals(Timestamp.class)){
                askDbType = LocalDateTime.class;
                convertFunc = data->Timestamp.valueOf((LocalDateTime) data);
            }
            if (parameterType.isPrimitive()) {
                //by the index of the column
                lastColumnValue = row.get((int) args[0] - 1, askDbType);
            } else {
                //by the name of the column
                lastColumnValue = row.get((String) args[0], askDbType);
            }
            //adapter type convert function invoke
            if(lastColumnValue!=null&&convertFunc!=null){
                lastColumnValue = convertFunc.apply(lastColumnValue);
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

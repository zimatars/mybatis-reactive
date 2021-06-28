package com.waterdrop.mybatisreactive.toolkit;

import io.r2dbc.spi.IsolationLevel;
import org.apache.ibatis.session.TransactionIsolationLevel;

public class TransactionConvert {
    public static IsolationLevel TransactionIsolationLevelConvertIsolationLevel(TransactionIsolationLevel transactionIsolationLevel){
        switch (transactionIsolationLevel){
            case READ_UNCOMMITTED:
                return IsolationLevel.READ_UNCOMMITTED;
            case READ_COMMITTED:
                return IsolationLevel.READ_COMMITTED;
            case REPEATABLE_READ:
                return IsolationLevel.REPEATABLE_READ;
            case SERIALIZABLE:
                return IsolationLevel.SERIALIZABLE;
            default:
                throw new IllegalArgumentException("transactionIsolationLevel unknown:"+transactionIsolationLevel.getLevel());
        }
    }
}

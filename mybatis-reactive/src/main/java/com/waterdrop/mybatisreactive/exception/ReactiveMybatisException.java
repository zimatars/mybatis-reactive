package com.waterdrop.mybatisreactive.exception;

public class ReactiveMybatisException extends RuntimeException{
    public ReactiveMybatisException() {
    }

    public ReactiveMybatisException(String message) {
        super(message);
    }

    public ReactiveMybatisException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReactiveMybatisException(Throwable cause) {
        super(cause);
    }

    public ReactiveMybatisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

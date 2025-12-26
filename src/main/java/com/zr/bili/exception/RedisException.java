package com.zr.bili.exception;

public class RedisException extends BaseException {
    //redis宕机问题
    public RedisException(String message) {
        super(message,1100);
    }
}

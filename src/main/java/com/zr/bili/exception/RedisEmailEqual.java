package com.zr.bili.exception;

public class RedisEmailEqual extends BaseException {

    public RedisEmailEqual(String message) {
        super(message, 1003);
    }
}

package com.zr.bili.exception;

public class redisEmailEmpty extends BaseException {
    public redisEmailEmpty(String message) {
        super(message, 1001);
    }
}

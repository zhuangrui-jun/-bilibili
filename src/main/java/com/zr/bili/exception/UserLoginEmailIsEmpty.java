package com.zr.bili.exception;

public class UserLoginEmailIsEmpty extends BaseException {
    public UserLoginEmailIsEmpty(String message) {
        super(message, 1004);
    }
}

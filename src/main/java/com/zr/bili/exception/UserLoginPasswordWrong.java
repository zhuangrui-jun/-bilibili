package com.zr.bili.exception;

public class UserLoginPasswordWrong extends BaseException {
    public UserLoginPasswordWrong(String message) {
        super(message, 1005);
    }
}

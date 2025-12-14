package com.zr.bili.exception;

/**
 * 用户注册相关业务异常
 */
public class UserRegisterIsEmpty extends BaseException {
    public UserRegisterIsEmpty(String message) {
        super(message, 1002);
    }
}

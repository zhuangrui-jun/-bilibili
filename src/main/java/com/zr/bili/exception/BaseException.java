package com.zr.bili.exception;

public class BaseException extends RuntimeException {

    private Integer code;
    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
    public BaseException(String msg,Integer code) {
        super(msg);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}

package com.supos.common.exception;

public class AppException extends BuzException {


    public AppException(int code, String msg) {
        super(code, msg);
    }

    public AppException(String msg) {
        super(msg);
    }
}

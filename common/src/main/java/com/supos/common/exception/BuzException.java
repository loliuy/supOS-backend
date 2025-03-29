package com.supos.common.exception;

import lombok.Data;

@Data
public class BuzException extends RuntimeException {

    private int code;

    private String msg;

    private Object[] params;

    public BuzException(int code, String msg, Object... params) {
        this.code = code;
        this.msg = msg;
        this.params = params;
    }


    public BuzException(String msg, Object... params) {
        super(msg);
        this.msg = msg;
        this.params = params;
    }
}

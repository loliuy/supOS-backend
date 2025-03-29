package com.supos.uns.exception;

import lombok.Data;

/**
 * 建议使用 common包 BuzException
 */
@Data
@Deprecated
public class BussinessException extends RuntimeException {

    private int code;

    private String msg;

    public BussinessException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BussinessException(String msg) {
        super(msg);
    }
}

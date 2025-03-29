package com.supos.common.exception;

public class NodeRedException extends BuzException {

    public NodeRedException(int code, String msg, Object... params) {
        super(code, msg, params);
    }

    public NodeRedException(String msg, Object... params) {
        super(msg, params);
    }
}

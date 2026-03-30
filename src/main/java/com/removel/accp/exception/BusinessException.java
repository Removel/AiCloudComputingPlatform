package com.removel.accp.exception;

import lombok.Getter;

public class BusinessException extends RuntimeException {
    @Getter
    private Integer code;
    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
    }
    public BusinessException(String message) {
        super(message);
    }
}

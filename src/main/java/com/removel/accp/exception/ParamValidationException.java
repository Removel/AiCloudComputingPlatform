package com.removel.accp.exception;

import lombok.Getter;

public class ParamValidationException extends RuntimeException {
    @Getter
    private Integer code;
    public ParamValidationException(String message, Integer code) {
        super(message);
        this.code = code;
    }
    public ParamValidationException(String message) {
        super(message);
    }
}

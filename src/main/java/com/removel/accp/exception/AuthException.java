package com.removel.accp.exception;

import lombok.Getter;

public class AuthException extends RuntimeException {

    @Getter
    private Integer code;
    public AuthException(String message, Integer code) {
        super(message);
        this.code = code;
    }
    public AuthException(String message) {
        super(message);
    }
}

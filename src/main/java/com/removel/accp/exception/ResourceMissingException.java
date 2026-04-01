package com.removel.accp.exception;

import lombok.Getter;

public class ResourceMissingException extends RuntimeException {
    @Getter
    private Integer code;
    public ResourceMissingException(String message, Integer code) {
        super(message);
        this.code = code;
    }
    public ResourceMissingException(String message) {
        super(message);
    }}

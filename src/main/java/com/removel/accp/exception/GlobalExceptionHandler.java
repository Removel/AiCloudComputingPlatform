package com.removel.accp.exception;

import com.removel.accp.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public Result<String> authExceptionHandler(AuthException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

}

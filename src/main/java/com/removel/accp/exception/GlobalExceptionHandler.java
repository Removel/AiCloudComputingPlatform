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

    @ExceptionHandler(ParamValidationException.class)
    public Result<String> ParamValidationExceptionHandler(ParamValidationException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<String> BusinessExceptionHandler(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常");
    }
}

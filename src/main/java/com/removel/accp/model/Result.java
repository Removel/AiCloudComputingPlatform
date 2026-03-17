package com.removel.accp.model;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Result(T data) {
        this.data = data;
        this.code = 200;
        this.message = "success";
    }

    public Result() {
        this.code = 200;
        this.message = "success";
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    // 成功携带数据返回对象方法
    public static <T> Result<T> success(T data) {
        return new Result<T>(data);
    }

    // 成功不携带数据返回对象方法
    public static <T> Result<T> success() {
        return new Result<>();
    }

    // 失败数据返回对象方法
    public static <T> Result<T> fail(Integer code,String message) {
        return new Result<T>(code, message);
    }
}

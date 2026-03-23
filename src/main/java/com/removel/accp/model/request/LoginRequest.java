package com.removel.accp.model.request;

import lombok.Data;

// 登陆请求类
@Data
public class LoginRequest {
    // 登陆方式 需要指定loginType
    private String loginType;   //值为：username、email、phone
    // 用户名、邮箱、手机号 与longinType对应
    private String username;
    private String email;
    private Integer phone;
    // 密码 必要
    private String password;
}

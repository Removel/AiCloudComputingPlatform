package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;

public interface IUserService extends IService<User> {

    User login(LoginRequest loginRequest);

    void register(RegisterRequest registerRequest);

    void sendCode(RegisterRequest registerRequest);

    void logout(Integer id,String token);
}

package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.LoginRequest;
import com.removel.accp.model.request.RegisterRequest;
import com.removel.accp.model.response.UserResponse;

public interface IUserService extends IService<User> {

    String login(LoginRequest loginRequest);

    void register(RegisterRequest registerRequest);

    void sendCode(RegisterRequest registerRequest);

    void logout(Integer id,String token);

    User getUserInfo(String token);

    void deleteUser(Integer id);

    void updateUser(User user);

    void deductRemainingComputePower(Integer id, Integer cost);
}

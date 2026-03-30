package com.removel.accp.controller;

import com.removel.accp.annotation.LogOperation;
import com.removel.accp.model.Result;
import com.removel.accp.model.entity.User;
import com.removel.accp.model.request.UserRequest;
import com.removel.accp.model.response.UserResponse;
import com.removel.accp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    // 构造器方法注入字段
    private final IUserService iUserService;
    @Autowired
    public UserController(IUserService iUserService) {
        this.iUserService = iUserService;
    }

    // 请求用户信息接口，对应查
    @LogOperation
    @PostMapping("/info")
    public Result<UserResponse> getUserInfo(@RequestHeader("Authorization") String token){
        log.info("请求用户信息，用户token为：{}",token);
        User user= iUserService.getUserInfo(token);
        UserResponse userResponse = new UserResponse();
        user.setPassword(null);
        userResponse.setUserInfo(user);
        log.info("请求用户信息成功，用户信息为：{}",userResponse);
        return Result.success(userResponse);
    }

    // 删除用户信息接口，逻辑上并不删除，而是将状态修改为停用，对应删
    @LogOperation
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Integer id){
        log.info("删除用户信息，用户id为：{}",id);
        iUserService.deleteUser(id);
        log.info("删除用户信息成功");
        return Result.success();
    }

    // 修改用户信息接口，对应改
    @LogOperation
    @PutMapping("/update")
    public Result<?> updateUser(@RequestBody UserRequest userRequest){
        log.info("修改用户信息，用户信息为：{}",userRequest.getUserUpdateInfo());
        iUserService.updateUser(userRequest.getUserUpdateInfo());
        log.info("修改用户信息成功");
        return Result.success();
    }


}

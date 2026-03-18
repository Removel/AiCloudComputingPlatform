package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.mapper.UserMapper;
import com.removel.accp.model.entity.User;
import com.removel.accp.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private UserMapper userMapper;
    @Autowired
    public IUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

}

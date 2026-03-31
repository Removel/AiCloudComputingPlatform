package com.removel.accp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.removel.accp.mapper.ServerMachineMapper;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.service.IServerMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IServerMachineServiceImpl extends ServiceImpl<ServerMachineMapper, ServerMachine> implements IServerMachineService{
}

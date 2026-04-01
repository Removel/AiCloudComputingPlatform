package com.removel.accp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.entity.ServerSpec;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.request.ServerMachineRequest;

import java.util.List;


public interface IServerMachineService extends IService<ServerMachine> {

    void addMachine(ServerMachine serverMachine);

    void deleteMachine(Long id);

    void updateMachine(ServerMachine serverMachine);

    ServerMachine getMachineById(Long id);

    List<ServerMachine> getMachineList(Integer page, Integer size, ServerMachine serverMachineTemplate);

    void updateMachineStatus(Long id, Status status);

    ServerMachine selectMachineByIdWithPessimisticLock(Long id);


}

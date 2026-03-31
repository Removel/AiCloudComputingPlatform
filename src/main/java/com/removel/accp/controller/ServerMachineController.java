package com.removel.accp.controller;

import com.removel.accp.model.Result;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.enums.Status;
import com.removel.accp.model.request.ServerMachineRequest;
import com.removel.accp.model.response.ServerMachineResponse;
import com.removel.accp.service.IServerMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/machine")
public class ServerMachineController {

    private final IServerMachineService serverMachineService;
    @Autowired
    public ServerMachineController(IServerMachineService serverMachineService) {
        this.serverMachineService = serverMachineService;
    }

    //增加单个服务器
    @PostMapping()
    public Result<?> addMachine(@RequestBody ServerMachineRequest serverMachineRequest){
        log.info("增加单个服务器，增加对象为：{}",serverMachineRequest.getNewServerMachine());
        serverMachineService.addMachine(serverMachineRequest.getNewServerMachine());
        log.info("增加成功");
        return Result.success();
    }

    //删除单个服务器
    @DeleteMapping("/{id}")
    public Result<?> deleteMachine(@PathVariable Long id){
        log.info("删除单个服务器，删除对象为：{}",id);
        serverMachineService.deleteMachine(id);
        log.info("删除成功");
        return Result.success();
    }

    //更新单个服务器信息
    @PutMapping()
    public Result<?> updateMachine(@RequestBody ServerMachineRequest serverMachineRequest){
        log.info("更新单个服务器，更新对象id为：{}，更新内容为：{}",serverMachineRequest.getUpdateServerMachine().getId(),serverMachineRequest.getUpdateServerMachine());
        serverMachineService.updateMachine(serverMachineRequest.getUpdateServerMachine());
        log.info("更新成功");
        return Result.success();
    }

    //根据id查询单个服务器
    @GetMapping("/{id}")
    public Result<ServerMachineResponse> getMachineById(@PathVariable Long id){
        log.info("根据id查询单个服务器，查询对象id为：{}",id);
        ServerMachineResponse serverMachineResponse = new ServerMachineResponse();
        serverMachineResponse.setServerMachine(serverMachineService.getMachineById(id));
        log.info("查询成功");
        return Result.success(serverMachineResponse);
    }

    //分页查询服务器列表
    @GetMapping("/list")
    public Result<ServerMachineResponse> getMachineList(@RequestParam Integer page , @RequestParam Integer size , @RequestBody ServerMachineRequest serverMachineRequest){
        log.info("分页查询服务器列表，查询服务器模板对象为：{}",serverMachineRequest.getServerMachineTemplate());
        ServerMachineResponse serverMachineResponse = new ServerMachineResponse();
        serverMachineResponse.setServerMachineList(serverMachineService.getMachineList(page,size,serverMachineRequest.getServerMachineTemplate()));
        log.info("查询成功");
        return Result.success(serverMachineResponse);
    }

    //修改服务器上下架状态
    @PutMapping("/status")
    public Result<?> updateMachineStatus(@RequestParam Long id , @RequestParam Status status){
        log.info("修改服务器上下架状态，修改对象id为：{}，修改状态为：{}",id,status);
        serverMachineService.updateMachineStatus(id,status);
        log.info("修改成功");
        return Result.success();
    }


}

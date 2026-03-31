package com.removel.accp.model.response;

import com.removel.accp.model.entity.ServerMachine;
import lombok.Data;

import java.util.List;

@Data
public class ServerMachineResponse {

    //单个查询结果
    private ServerMachine serverMachine;

    //批量查询结果
    private List<ServerMachine> serverMachines;
}

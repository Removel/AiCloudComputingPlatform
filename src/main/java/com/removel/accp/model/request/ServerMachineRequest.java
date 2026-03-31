package com.removel.accp.model.request;

import com.removel.accp.model.entity.ServerMachine;
import lombok.Data;

@Data
public class ServerMachineRequest {

    //add请求对应信息
    private ServerMachine newServerMachine;

    //update请求对应信息
    private ServerMachine updateServerMachine;

    //list请求对应信息
    private ServerMachine serverMachineTemplate;
}


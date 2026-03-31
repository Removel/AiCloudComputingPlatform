package com.removel.accp.model.entity;

import lombok.Data;

@Data
public class ServerSpec {
    /**
     * 服务器详细配置类对象，被包含在服务器类当中，一般不单独使用
     */
    // CPU核心数
    private Integer cpuCore;

    // 内存 GB
    private Integer memoryGb;

    // GPU型号
    private String gpuModel;

    // 显存 GB
    private Integer gpuMemoryGb;

    // 磁盘 GB
    private Integer diskGb;

    // 操作系统
    private String os;
}

package com.removel.accp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.entity.Session;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}

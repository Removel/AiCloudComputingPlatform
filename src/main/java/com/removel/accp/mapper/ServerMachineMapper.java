package com.removel.accp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.removel.accp.model.entity.ServerMachine;
import com.removel.accp.model.enums.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ServerMachineMapper extends BaseMapper<ServerMachine> {

    // 悲观锁查询
    @Select("select * from server_machine where id = #{id} for update")
    ServerMachine selectByIdForUpdate(Long id);

}

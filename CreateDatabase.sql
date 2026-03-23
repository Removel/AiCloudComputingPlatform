# 用户表
CREATE TABLE `user` (
                        `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键id，自增',
                        `name` VARCHAR(50) NOT NULL COMMENT '用户名',
                        `password` VARCHAR(255) NOT NULL COMMENT '密码',
                        `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
                        `role` VARCHAR(10) NOT NULL DEFAULT 'COMMON' COMMENT '角色：ADMIN-管理，COMMON-一般用户',
                        `remaining_compute_power` INT DEFAULT 0 COMMENT '剩余算力',
                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `status` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，DISABLED-停用，EXPIRED-过期，LOCKED-锁定，CANCELLED-注销',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_name` (`name`),
                        UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
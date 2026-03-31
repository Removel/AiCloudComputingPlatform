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

# 服务器机器表
CREATE TABLE `server_machine` (
                                  `id` BIGINT NOT NULL COMMENT '主键ID',
                                  `server_name` VARCHAR(100) DEFAULT NULL COMMENT '服务器名称/标识',
                                  `specification` JSON DEFAULT NULL COMMENT '服务器规格描述（如：NVIDIA A100 80GB），使用json格式记录',
                                  `total_compute_power` INT DEFAULT NULL COMMENT '总算力额度（单位：算力点）',
                                  `occupied_compute_power` INT DEFAULT NULL COMMENT '已占用算力额度',
                                  `status` VARCHAR(20) DEFAULT NULL COMMENT '服务器状态状态',
                                  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
                                  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器机器表';

# 商品表
# 该表会用于商品秒杀，故不会使用外键
CREATE TABLE `server_product` (
                                  `id` BIGINT NOT NULL COMMENT '主键ID',
                                  `machine_id` BIGINT DEFAULT NULL COMMENT '所属服务器ID（关联server_machine表的id）',
                                  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
                                  `rental_hours` INT DEFAULT NULL COMMENT '租赁时长（单位：小时）',
                                  `consume_compute_power` INT DEFAULT NULL COMMENT '消耗算力',
                                  `price` INT DEFAULT NULL COMMENT '商品价格（单位：分）',
                                  `scene_desc` VARCHAR(500) DEFAULT NULL COMMENT '适用场景描述',
                                  `status` VARCHAR(20) DEFAULT NULL COMMENT '商品状态',
                                  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
                                  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_machine_id` (`machine_id`) COMMENT '所属服务器ID索引',
                                  KEY `idx_status` (`status`) COMMENT '商品状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器租赁商品表，代表一个租赁套餐';
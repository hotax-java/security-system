-- 创建GitHub用户表
CREATE TABLE IF NOT EXISTS `sys_github_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '系统用户ID（可为空，表示未绑定）',
  `github_id` bigint(20) NOT NULL COMMENT 'GitHub用户ID',
  `login` varchar(100) NOT NULL COMMENT 'GitHub登录名',
  `name` varchar(100) DEFAULT NULL COMMENT 'GitHub用户名',
  `email` varchar(100) DEFAULT NULL COMMENT 'GitHub邮箱',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  `location` varchar(255) DEFAULT NULL COMMENT '位置',
  `company` varchar(100) DEFAULT NULL COMMENT '公司',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_github_id` (`github_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GitHub用户表'; 
-- ========================================
-- 创建系统用户表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status INT DEFAULT 1 COMMENT '用户状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted INT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ========================================
-- 创建系统角色表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '角色状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted INT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ========================================
-- 创建系统权限表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    perm_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    description VARCHAR(255) COMMENT '权限描述',
    perm_type INT DEFAULT 1 COMMENT '权限类型：1-菜单，2-按钮，3-接口',
    parent_id BIGINT COMMENT '父权限ID',
    perm_path VARCHAR(255) COMMENT '权限路径',
    status INT DEFAULT 1 COMMENT '权限状态：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted INT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- ========================================
-- 创建用户角色关联表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    UNIQUE KEY uk_user_role (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ========================================
-- 创建角色权限关联表
-- ========================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ========================================
-- 插入初始数据
-- ========================================

-- 插入初始管理员用户
INSERT INTO sys_user (username, password, real_name, email, status, create_by) VALUES 
('admin', '$2a$10$x4/GrFOigkEPIqUO53M5VOGmQFmzVX.tIrHk5tW/5EdmnCZVEYEm2', '系统管理员', 'admin@example.com', 1, 'system');

-- 插入初始角色
INSERT INTO sys_role (role_code, role_name, description, status, create_by) VALUES 
('ADMIN', '系统管理员', '系统管理员角色，拥有所有权限', 1, 'system'),
('USER', '普通用户', '普通用户角色', 1, 'system');

-- 插入初始权限
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, perm_path, sort_order, create_by) VALUES 
-- 用户管理
('USER_MANAGE', '用户管理', '用户管理模块', 1, NULL, '/users', 1, 'system'),
('USER_QUERY', '用户查询', '查询用户信息', 2, 1, NULL, 1, 'system'),
('USER_CREATE', '用户创建', '创建新用户', 2, 1, NULL, 2, 'system'),
('USER_UPDATE', '用户更新', '更新用户信息', 2, 1, NULL, 3, 'system'),
('USER_DELETE', '用户删除', '删除用户', 2, 1, NULL, 4, 'system'),

-- 角色管理
('ROLE_MANAGE', '角色管理', '角色管理模块', 1, NULL, '/roles', 2, 'system'),
('ROLE_QUERY', '角色查询', '查询角色信息', 2, 6, NULL, 1, 'system'),
('ROLE_CREATE', '角色创建', '创建新角色', 2, 6, NULL, 2, 'system'),
('ROLE_UPDATE', '角色更新', '更新角色信息', 2, 6, NULL, 3, 'system'),
('ROLE_DELETE', '角色删除', '删除角色', 2, 6, NULL, 4, 'system'),

-- 权限管理
('PERMISSION_MANAGE', '权限管理', '权限管理模块', 1, NULL, '/permissions', 3, 'system'),
('PERMISSION_QUERY', '权限查询', '查询权限信息', 2, 11, NULL, 1, 'system'),
('PERMISSION_CREATE', '权限创建', '创建新权限', 2, 11, NULL, 2, 'system'),
('PERMISSION_UPDATE', '权限更新', '更新权限信息', 2, 11, NULL, 3, 'system'),
('PERMISSION_DELETE', '权限删除', '删除权限', 2, 11, NULL, 4, 'system');

-- 给管理员用户分配管理员角色
INSERT INTO sys_user_role (user_id, role_id, create_by) VALUES 
(1, 1, 'system');

-- 给管理员角色分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id, create_by) 
SELECT 1, permission_id, 'system' FROM sys_permission;
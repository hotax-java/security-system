-- 开始事务
START TRANSACTION;

-- 安全地添加icon列（不使用存储过程，避免DELIMITER问题）
-- 先检查列是否存在
SET @columnExists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_permission'
    AND COLUMN_NAME = 'icon'
);

-- 如果列不存在，则添加列
SET @sql = IF(@columnExists = 0, 
    'ALTER TABLE sys_permission ADD COLUMN icon VARCHAR(50) COMMENT \'菜单图标\'', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有菜单的图标信息
UPDATE sys_permission SET icon = 'DashboardOutlined' WHERE perm_code = 'DASHBOARD';
UPDATE sys_permission SET icon = 'UserOutlined' WHERE perm_code = 'USER_MANAGE';
UPDATE sys_permission SET icon = 'TeamOutlined' WHERE perm_code = 'ROLE_MANAGE';
UPDATE sys_permission SET icon = 'SafetyOutlined' WHERE perm_code = 'PERMISSION_MANAGE';
UPDATE sys_permission SET icon = 'ApiOutlined' WHERE perm_code = 'OPENAPI_MANAGE';
UPDATE sys_permission SET icon = 'KeyOutlined' WHERE perm_code = 'OPENAPI_CREDENTIAL';
UPDATE sys_permission SET icon = 'AppstoreOutlined' WHERE perm_code = 'OPENAPI_RESOURCE';
UPDATE sys_permission SET icon = 'SafetyOutlined' WHERE perm_code = 'OPENAPI_PERMISSION';

-- 提交事务
COMMIT;
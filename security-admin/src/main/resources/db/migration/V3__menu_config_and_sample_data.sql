-- 删除可能已经存在的数据
DELETE FROM sys_role_permission WHERE permission_id IN (SELECT permission_id FROM sys_permission WHERE perm_code IN ('OPENAPI_MANAGE', 'OPENAPI_CREDENTIAL', 'OPENAPI_RESOURCE', 'OPENAPI_PERMISSION'));
DELETE FROM sys_permission WHERE perm_code IN ('OPENAPI_MANAGE', 'OPENAPI_CREDENTIAL', 'OPENAPI_RESOURCE', 'OPENAPI_PERMISSION');
DELETE FROM sys_permission WHERE perm_code IN ('OPENAPI_CREDENTIAL_QUERY', 'OPENAPI_CREDENTIAL_CREATE', 'OPENAPI_CREDENTIAL_UPDATE', 'OPENAPI_CREDENTIAL_DELETE');
DELETE FROM sys_permission WHERE perm_code IN ('OPENAPI_RESOURCE_QUERY', 'OPENAPI_RESOURCE_CREATE', 'OPENAPI_RESOURCE_UPDATE', 'OPENAPI_RESOURCE_DELETE');
DELETE FROM sys_permission WHERE perm_code IN ('OPENAPI_PERMISSION_QUERY', 'OPENAPI_PERMISSION_ASSIGN');
DELETE FROM sys_resource WHERE resource_code LIKE 'order:%';
DELETE FROM sys_resource WHERE resource_code LIKE 'customer:%';
DELETE FROM sys_resource WHERE resource_code LIKE 'product:%';

-- 插入OpenAPI管理父菜单
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, perm_path, status, sort_order, create_by) 
VALUES ('OPENAPI_MANAGE', 'OpenAPI管理', 'OpenAPI接口管理', 1, NULL, '/openapi', 1, 5, 'system');

-- 获取插入的父菜单ID
SET @openapi_menu_id = LAST_INSERT_ID();

-- 插入客户端凭证子菜单
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, perm_path, status, sort_order, create_by) 
VALUES ('OPENAPI_CREDENTIAL', '客户端凭证', '管理API调用凭证', 1, @openapi_menu_id, '/openapi/credentials', 1, 1, 'system');

-- 获取客户端凭证菜单ID
SET @credential_menu_id = LAST_INSERT_ID();

-- 插入API资源子菜单
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, perm_path, status, sort_order, create_by) 
VALUES ('OPENAPI_RESOURCE', 'API资源', '管理API接口资源', 1, @openapi_menu_id, '/openapi/resources', 1, 2, 'system');

-- 获取API资源菜单ID
SET @resource_menu_id = LAST_INSERT_ID();

-- 插入API权限分配子菜单
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, perm_path, status, sort_order, create_by) 
VALUES ('OPENAPI_PERMISSION', 'API权限分配', '为客户端凭证分配API权限', 1, @openapi_menu_id, '/openapi/permissions', 1, 3, 'system');

-- 获取API权限分配菜单ID
SET @permission_menu_id = LAST_INSERT_ID();

-- 客户端凭证功能权限（作为客户端凭证菜单的子项）
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, status, sort_order, create_by)
VALUES 
('OPENAPI_CREDENTIAL_QUERY', '查询客户端凭证', '允许查询客户端凭证列表', 2, @credential_menu_id, 1, 1, 'system'),
('OPENAPI_CREDENTIAL_CREATE', '创建客户端凭证', '允许创建新的客户端凭证', 2, @credential_menu_id, 1, 2, 'system'),
('OPENAPI_CREDENTIAL_UPDATE', '更新客户端凭证', '允许更新客户端凭证信息', 2, @credential_menu_id, 1, 3, 'system'),
('OPENAPI_CREDENTIAL_DELETE', '删除客户端凭证', '允许删除客户端凭证', 2, @credential_menu_id, 1, 4, 'system');

-- API资源功能权限（作为API资源菜单的子项）
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, status, sort_order, create_by)
VALUES 
('OPENAPI_RESOURCE_QUERY', '查询API资源', '允许查询API资源列表', 2, @resource_menu_id, 1, 1, 'system'),
('OPENAPI_RESOURCE_CREATE', '创建API资源', '允许创建新的API资源', 2, @resource_menu_id, 1, 2, 'system'),
('OPENAPI_RESOURCE_UPDATE', '更新API资源', '允许更新API资源信息', 2, @resource_menu_id, 1, 3, 'system'),
('OPENAPI_RESOURCE_DELETE', '删除API资源', '允许删除API资源', 2, @resource_menu_id, 1, 4, 'system');

-- API权限分配功能权限（作为API权限分配菜单的子项）
INSERT INTO sys_permission (perm_code, perm_name, description, perm_type, parent_id, status, sort_order, create_by)
VALUES 
('OPENAPI_PERMISSION_QUERY', '查询API权限', '允许查询API权限分配情况', 2, @permission_menu_id, 1, 1, 'system'),
('OPENAPI_PERMISSION_ASSIGN', '分配API权限', '允许为客户端凭证分配API权限', 2, @permission_menu_id, 1, 2, 'system');

-- 获取管理员角色ID
SET @admin_role_id = (SELECT role_id FROM sys_role WHERE role_code = 'ADMIN');

-- 为管理员角色分配菜单权限（包括父菜单和所有子菜单）
INSERT INTO sys_role_permission (role_id, permission_id, create_by) 
SELECT @admin_role_id, permission_id, 'system'
FROM sys_permission 
WHERE perm_code IN ('OPENAPI_MANAGE', 'OPENAPI_CREDENTIAL', 'OPENAPI_RESOURCE', 'OPENAPI_PERMISSION');

-- 为管理员角色分配功能权限
INSERT INTO sys_role_permission (role_id, permission_id, create_by)
SELECT @admin_role_id, permission_id, 'system'
FROM sys_permission
WHERE perm_code IN (
    'OPENAPI_CREDENTIAL_QUERY', 'OPENAPI_CREDENTIAL_CREATE', 'OPENAPI_CREDENTIAL_UPDATE', 'OPENAPI_CREDENTIAL_DELETE',
    'OPENAPI_RESOURCE_QUERY', 'OPENAPI_RESOURCE_CREATE', 'OPENAPI_RESOURCE_UPDATE', 'OPENAPI_RESOURCE_DELETE',
    'OPENAPI_PERMISSION_QUERY', 'OPENAPI_PERMISSION_ASSIGN'
);

-- 添加订单管理相关API资源
INSERT INTO sys_resource (resource_code, resource_name, resource_path, method, qps_limit, status, create_by) VALUES
('order:query', '订单查询', '/api/v1/orders/**', 'GET', 100, 1, 'system'),
('order:create', '订单创建', '/api/v1/orders', 'POST', 50, 1, 'system'),
('order:update', '订单更新', '/api/v1/orders/*', 'PUT', 50, 1, 'system'),
('order:delete', '订单删除', '/api/v1/orders/*', 'DELETE', 20, 1, 'system'),
('order:detail', '订单详情', '/api/v1/orders/*/detail', 'GET', 200, 1, 'system');

-- 添加客户管理相关API资源
INSERT INTO sys_resource (resource_code, resource_name, resource_path, method, qps_limit, status, create_by) VALUES
('customer:query', '客户查询', '/api/v1/customers/**', 'GET', 100, 1, 'system'),
('customer:create', '客户创建', '/api/v1/customers', 'POST', 30, 1, 'system'),
('customer:update', '客户更新', '/api/v1/customers/*', 'PUT', 30, 1, 'system'),
('customer:delete', '客户删除', '/api/v1/customers/*', 'DELETE', 10, 1, 'system');

-- 添加商品管理相关API资源
INSERT INTO sys_resource (resource_code, resource_name, resource_path, method, qps_limit, status, create_by) VALUES
('product:query', '商品查询', '/api/v1/products/**', 'GET', 500, 1, 'system'),
('product:create', '商品创建', '/api/v1/products', 'POST', 20, 1, 'system'),
('product:update', '商品更新', '/api/v1/products/*', 'PUT', 30, 1, 'system'),
('product:delete', '商品删除', '/api/v1/products/*', 'DELETE', 10, 1, 'system');

-- 创建默认API凭证
INSERT INTO sys_client_credential (app_id, app_secret, client_id, status, remark, create_by) VALUES
('admin-api-default', '$2a$10$JGV1apqlQoFVpQzZNAd4kOVNpOTZZ.Yg5jGGpWQmNFGiCtn0omZni', 'openapi', 1, '系统默认API凭证', 'system');

-- 为默认凭证分配API资源权限
SET @default_credential_id = (SELECT id FROM sys_client_credential WHERE app_id = 'admin-api-default');
INSERT INTO sys_credential_resource_rel (credential_id, resource_id, create_by)
SELECT @default_credential_id, resource_id, 'system'
FROM sys_resource;
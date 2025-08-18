import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Space,
  Tag,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Checkbox,
  Divider
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { userService, roleService } from '../../services';
import Permission from '../../components/common/Permission';
import { PermissionUtil } from '../../utils/permissionUtil';

const { Option } = Select;
const { Search } = Input;

interface User {
  userId: number;
  username: string;
  realName: string;
  email: string;
  phone: string;
  status: number;
  createTime: string;
  roles?: Role[];
}

interface Role {
  roleId: number;
  roleName: string;
  roleCode: string;
}

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<number[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number, range: number[]) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
  });

  const [form] = Form.useForm();
  const [roleForm] = Form.useForm();

  useEffect(() => {
    loadUsers();
    loadRoles();
  }, [pagination.current, pagination.pageSize, searchKeyword]);

  const loadUsers = async () => {
    setLoading(true);
    try {
    
      const result = await userService.getUsers(pagination.current, pagination.pageSize, searchKeyword);
      // 处理返回结果，适应新的响应格式
      if (result && typeof result === 'object') {
        // 如果返回的是分页对象
        if ('list' in result && 'total' in result) {
          const userList: User[] = Array.isArray(result.list) ? result.list : [];
          setUsers(userList);
          setPagination(prev => ({
            ...prev,
            total: typeof result.total === 'number' ? result.total : 0
          }));
        } 
        // 如果返回的是数组
        else if (Array.isArray(result)) {
          setUsers(result as User[]);
          setPagination(prev => ({
            ...prev,
            total: result.length
          }));
        } else {
          setUsers([]);
          setPagination(prev => ({
            ...prev,
            total: 0
          }));
        }
      } else {
        setUsers([]);
        setPagination(prev => ({
          ...prev,
          total: 0
        }));
      }
    } catch (error) {
      message.error('加载用户列表失败');
      console.error('加载用户列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadRoles = async () => {
    try {
      // 使用新的 getAllRoles 方法获取所有角色
      const result = await roleService.getAllRoles();
      // 确保结果是数组，并进行类型转换
      const rolesList: Role[] = Array.isArray(result) ? result : [];
      setRoles(rolesList);
    } catch (error) {
      message.error('加载角色列表失败');
      console.error('加载角色列表失败:', error);
    }
  };

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    setShowModal(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue({
      username: user.username,
      realName: user.realName,
      email: user.email,
      phone: user.phone,
      status: user.status
    });
    setShowModal(true);
  };

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (editingUser) {
        await userService.updateUser(editingUser.userId, values);
        message.success('用户更新成功');
      } else {
        await userService.createUser(values);
        message.success('用户创建成功');
      }
      setShowModal(false);
      loadUsers();
    } catch (error) {
      message.error(editingUser ? '用户更新失败' : '用户创建失败');
      console.error('保存用户失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (userId: number) => {
    try {
      await userService.deleteUser(userId);
      message.success('用户删除成功');
      loadUsers();
    } catch (error) {
      message.error('用户删除失败');
      console.error('删除用户失败:', error);
    }
  };

  const handleToggleStatus = async (userId: number, status: number) => {
    try {
      // 计算新状态：如果当前是 1 (启用)，则变为 0 (禁用)，反之亦然
      const newStatus = status === 1 ? 0 : 1;
      console.log(`Toggling user ${userId} status from ${status} to ${newStatus}`);
      
      // 调用 API 更新用户状态
      await userService.toggleUserStatus(userId, newStatus);
      message.success('用户状态更新成功');
      loadUsers();
    } catch (error) {
      message.error('用户状态更新失败');
      console.error('更新用户状态失败:', error);
    }
  };

  const handleAssignRoles = async (userId: number) => {
    setCurrentUserId(userId);
    try {
      // 获取用户当前角色
      const userRoles = await userService.getUserRoles(userId);
      // 处理返回的角色数据，确保是角色ID数组
      let roleIds: number[] = [];
      if (Array.isArray(userRoles)) {
        // 如果返回的是对象数组，提取roleId
        if (userRoles.length > 0 && typeof userRoles[0] === 'object' && 'roleId' in userRoles[0]) {
          roleIds = userRoles.map((role: any) => role.roleId);
        } 
        // 如果返回的是ID数组，直接使用
        else if (userRoles.length > 0 && typeof userRoles[0] === 'number') {
          roleIds = userRoles as number[];
        }
        // 如果返回的是角色代码字符串数组，需要查找对应的角色ID
        else if (userRoles.length > 0 && typeof userRoles[0] === 'string') {
          // 假设roles已经加载，包含所有角色信息
          const roleMap = new Map(roles.map(role => [role.roleCode, role.roleId]));
          roleIds = userRoles
            .map((roleCode: string) => roleMap.get(roleCode))
            .filter((id): id is number => id !== undefined);
        }
      }
      
      setSelectedRoles(roleIds);
      setShowRoleModal(true);
    } catch (error) {
      message.error('获取用户角色失败');
      console.error('获取用户角色失败:', error);
    }
  };

  const handleSaveRoles = async () => {
    if (currentUserId) {
      try {
        await userService.assignUserRoles(currentUserId, selectedRoles);
        message.success('角色分配成功');
        setShowRoleModal(false);
        loadUsers();
      } catch (error) {
        message.error('角色分配失败');
        console.error('分配角色失败:', error);
      }
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleTableChange = (paginationConfig: any) => {
    setPagination(prev => ({
      ...prev,
      current: paginationConfig.current,
      pageSize: paginationConfig.pageSize
    }));
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (text: string) => (
        <Space>
          <UserOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '真实姓名',
      dataIndex: 'realName',
      key: 'realName',
      width: 120,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 120,
      render: (time: string) => new Date(time).toLocaleDateString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: User) => (
        <Space size="small">
          <Permission code="USER_UPDATE">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
          </Permission>
          <Permission code="USER_UPDATE">
            <Button
              type="link"
              size="small"
              onClick={() => handleToggleStatus(record.userId, record.status)}
            >
              {record.status === 1 ? '禁用' : '启用'}
            </Button>
          </Permission>
          <Permission code="USER_UPDATE">
            <Button
              type="link"
              size="small"
              onClick={() => handleAssignRoles(record.userId)}
              disabled={record.status === 0}
            >
              分配角色
            </Button>
          </Permission>
          <Permission code="USER_DELETE">
            <Popconfirm
              title="确定要删除这个用户吗？"
              onConfirm={() => handleDelete(record.userId)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                删除
              </Button>
            </Popconfirm>
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
          <Col>
            <h2 style={{ margin: 0 }}>用户管理</h2>
          </Col>
          <Col>
            <Space>
              <Search
                placeholder="搜索用户名或姓名"
                allowClear
                enterButton={<SearchOutlined />}
                size="middle"
                onSearch={handleSearch}
                style={{ width: 250 }}
              />
              <Button
                type="default"
                icon={<ReloadOutlined />}
                onClick={loadUsers}
              >
                刷新
              </Button>
              <Permission code="USER_CREATE">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={handleCreate}
                >
                  新增用户
                </Button>
              </Permission>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="userId"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* 用户编辑模态框 */}
      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={showModal}
        onCancel={() => setShowModal(false)}
        footer={null}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 1 }}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}

          <Form.Item
            label="真实姓名"
            name="realName"
          >
            <Input placeholder="请输入真实姓名" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item
            label="状态"
            name="status"
          >
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setShowModal(false)}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                保存
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 角色分配模态框 */}
      <Modal
        title={<div style={{ borderBottom: '1px solid #f0f0f0', paddingBottom: 10, fontSize: 16, fontWeight: 500 }}>分配角色</div>}
        open={showRoleModal}
        onOk={handleSaveRoles}
        onCancel={() => setShowRoleModal(false)}
        okText="保存"
        cancelText="取消"
        width={700}
        centered
        styles={{ body: { padding: '10px 24px 12px' } }}
        okButtonProps={{ 
          style: { borderRadius: 4 },
          disabled: !PermissionUtil.hasPermission('USER_UPDATE') 
        }}
        cancelButtonProps={{ style: { borderRadius: 4 } }}
      >
        <div 
          style={{ 
            maxHeight: 400, 
            overflowY: 'auto', 
            overflowX: 'hidden',
            padding: '12px',
            background: '#f9f9f9',
            borderRadius: 6,
            marginTop: 5
          }}
        >
          <Checkbox.Group
            value={selectedRoles}
            onChange={setSelectedRoles}
            style={{ width: '100%' }}
          >
            <Row gutter={[20, 16]}>
              {roles.map((role) => (
                <Col span={6} key={role.roleId}>
                  <Checkbox 
                    value={role.roleId} 
                    style={{ 
                      width: '100%', 
                      whiteSpace: 'normal',
                      paddingLeft: 24
                    }}
                  >
                    <span style={{ fontWeight: role.roleCode === 'ADMIN' ? 500 : 400 }}>
                      {role.roleName}
                    </span>
                  </Checkbox>
                </Col>
              ))}
            </Row>
          </Checkbox.Group>
        </div>
      </Modal>
    </div>
  );
};

export default UserManagement; 
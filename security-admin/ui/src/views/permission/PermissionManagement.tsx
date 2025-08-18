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
  TreeSelect
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyOutlined,
  SearchOutlined,
  ReloadOutlined,
  FolderOutlined,
  FileOutlined
} from '@ant-design/icons';
import { permissionService } from '../../apis/permissionService';
import Permission from '../../components/common/Permission';
import { PermissionUtil } from '../../utils/permissionUtil';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

interface Permission {
  permissionId: number;
  permName: string;
  permCode: string;
  permType: number;  // 修正：后端返回的是number类型
  parentId: number;
  permPath: string;
  description?: string;  // 添加描述字段
  status: number;
  sortOrder: number;
  createTime: string;
  updateTime?: string;
  children?: Permission[];
}

const PermissionManagement: React.FC = () => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [permissionTree, setPermissionTree] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingPermission, setEditingPermission] = useState<Permission | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  const [form] = Form.useForm();

  useEffect(() => {
    loadPermissions();
  }, [searchKeyword]);

  const loadPermissions = async () => {
    setLoading(true);
    try {
      // 使用新的 getAllPermissions 方法获取所有权限
      const result = await permissionService.getAllPermissions();
      
      // console.log('API返回的原始数据:', result);
      
      // 确保结果是数组
      const permissionList: Permission[] = Array.isArray(result) ? result : [];
      
      // console.log('处理后的权限列表:', permissionList);
      
      // 构建树形结构
      const tree = buildPermissionTree(permissionList);
      
      // console.log('构建的树形结构:', tree);

      setPermissions(tree);
      
      // 构建TreeSelect数据
      const treeSelectData = buildTreeSelectData(permissionList);
      setPermissionTree(treeSelectData);
      
      // 默认展开所有节点
      const allKeys = permissionList.map((p: Permission) => p.permissionId);
      setExpandedRowKeys(allKeys);
    } catch (error: any) {
      console.error('加载权限列表失败:', error);
      if (error.response?.status === 403) {
        message.error('权限不足，无法查看权限列表');
      } else if (error.response?.status === 401) {
        message.error('未授权，请重新登录');
      } else {
        message.error('加载权限列表失败: ' + (error.message || '未知错误'));
      }
    } finally {
      setLoading(false);
    }
  };

  const buildPermissionTree = (permissions: Permission[]): Permission[] => {
    const map: { [key: number]: Permission } = {};
    const tree: Permission[] = [];

    // console.log('构建树形结构，原始数据:', permissions);

    // 创建映射
    permissions.forEach(permission => {
      map[permission.permissionId] = { ...permission, children: [] };
    });

    // 构建树结构
    permissions.forEach(permission => {
      // console.log(`处理权限 ${permission.permName}, parentId: ${permission.parentId}`);
      
      if (permission.parentId === 0 || permission.parentId === null || permission.parentId === undefined) {
        // 根节点
        // console.log(`添加根节点: ${permission.permName}`);
        tree.push(map[permission.permissionId]);
      } else if (map[permission.parentId]) {
        // 子节点
        // console.log(`添加子节点 ${permission.permName} 到父节点 ${map[permission.parentId].permName}`);
        map[permission.parentId].children!.push(map[permission.permissionId]);
      } else {
        // 如果找不到父节点，作为根节点处理
        // console.log(`找不到父节点，将 ${permission.permName} 作为根节点`);
        tree.push(map[permission.permissionId]);
      }
    });

    // console.log('构建完成的树形结构:', tree);
    return tree;
  };

  const buildTreeSelectData = (permissions: Permission[]) => {
    const tree: any[] = [];
    const map: { [key: number]: any } = {};

    // 添加根节点
    tree.push({
      title: '根目录',
      value: 0,
      key: 0,
      children: []
    });

    // 创建节点映射
    permissions.forEach(permission => {
      map[permission.permissionId] = {
        title: permission.permName,
        value: permission.permissionId,
        key: permission.permissionId,
        children: []
      };
    });

    // 构建树结构
    permissions.forEach(permission => {
      if (permission.parentId === 0 || permission.parentId === null) {
        tree[0].children.push(map[permission.permissionId]);
      } else if (map[permission.parentId]) {
        map[permission.parentId].children.push(map[permission.permissionId]);
      } else {
        // 如果找不到父节点，作为根节点的子节点
        tree[0].children.push(map[permission.permissionId]);
      }
    });

    return tree;
  };

  const handleCreate = () => {
    setEditingPermission(null);
    form.resetFields();
    setShowModal(true);
  };

  const handleEdit = (permission: Permission) => {
    setEditingPermission(permission);
    form.setFieldsValue({
      permName: permission.permName,
      permCode: permission.permCode,
      permType: permission.permType,
      parentId: permission.parentId,
      permPath: permission.permPath,
      sortOrder: permission.sortOrder,
      status: permission.status
    });
    setShowModal(true);
  };

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (editingPermission) {
        await permissionService.updatePermission(editingPermission.permissionId, values);
        message.success('权限更新成功');
      } else {
        await permissionService.createPermission(values);
        message.success('权限创建成功');
      }
      setShowModal(false);
      loadPermissions();
    } catch (error) {
      message.error(editingPermission ? '权限更新失败' : '权限创建失败');
      console.error('保存权限失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (permissionId: number) => {
    try {
      await permissionService.deletePermission(permissionId);
      message.success('权限删除成功');
      loadPermissions();
    } catch (error) {
      message.error('权限删除失败');
      console.error('删除权限失败:', error);
    }
  };

  const handleToggleStatus = async (permissionId: number, status: number) => {
    try {
      await permissionService.togglePermissionStatus(permissionId, status === 1 ? 0 : 1);
      message.success('权限状态更新成功');
      loadPermissions();
    } catch (error) {
      message.error('权限状态更新失败');
      console.error('更新权限状态失败:', error);
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
  };

  const getPermissionTypeTag = (type: number) => {
    const typeMap: { [key: number]: { color: string; text: string } } = {
      1: { color: 'blue', text: '菜单' },
      2: { color: 'green', text: '按钮' },
      3: { color: 'orange', text: 'API' }
    };
    const config = typeMap[type] || { color: 'default', text: `类型${type}` };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const getPermissionIcon = (type: number) => {
    switch (type) {
      case 1:
        return <FolderOutlined style={{ color: '#1890ff' }} />;
      case 2:
        return <SafetyOutlined style={{ color: '#52c41a' }} />;
      case 3:
        return <FileOutlined style={{ color: '#fa8c16' }} />;
      default:
        return <SafetyOutlined />;
    }
  };

  const columns = [
    {
      title: '权限名称',
      dataIndex: 'permName',
      key: 'permName',
      width: 200,
      render: (text: string, record: Permission) => (
        <Space>
          {getPermissionIcon(record.permType)}
          {text}
        </Space>
      ),
    },
    {
      title: '权限编码',
      dataIndex: 'permCode',
      key: 'permCode',
      width: 150,
      render: (text: string) => <Tag color="purple">{text}</Tag>,
    },
    {
      title: '权限类型',
      dataIndex: 'permType',
      key: 'permType',
      width: 100,
      render: (type: number) => getPermissionTypeTag(type),
    },
    {
      title: '路径',
      dataIndex: 'permPath',
      key: 'permPath',
      width: 150,
      ellipsis: true,
    },

    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80,
      sorter: (a: Permission, b: Permission) => a.sortOrder - b.sortOrder,
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
      width: 180,
      render: (_: any, record: Permission) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleToggleStatus(record.permissionId, record.status)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定要删除这个权限吗？"
            onConfirm={() => handleDelete(record.permissionId)}
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
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
          <Col>
            <h2 style={{ margin: 0 }}>权限管理 (共{permissions.length}条)</h2>
          </Col>
          <Col>
            <Space>
              <Search
                placeholder="搜索权限名称或编码"
                allowClear
                enterButton={<SearchOutlined />}
                size="middle"
                onSearch={handleSearch}
                style={{ width: 250 }}
              />
              <Button
                type="default"
                icon={<ReloadOutlined />}
                onClick={loadPermissions}
              >
                刷新
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增权限
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={permissions}
          rowKey="permissionId"
          loading={loading}
          pagination={false}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
            childrenColumnName: 'children',
            defaultExpandAllRows: true, // 默认展开所有节点
          }}
          scroll={{ x: 1200 }}
          // onRow={(record) => ({
          //   onClick: () => console.log('点击行:', record)
          // })}
        />
      </Card>

      {/* 权限编辑模态框 */}
      <Modal
        title={editingPermission ? '编辑权限' : '新增权限'}
        open={showModal}
        onCancel={() => setShowModal(false)}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 1, sortOrder: 1, parentId: 0 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="权限名称"
                name="permName"
                rules={[{ required: true, message: '请输入权限名称' }]}
              >
                <Input placeholder="请输入权限名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="权限编码"
                name="permCode"
                rules={[{ required: true, message: '请输入权限编码' }]}
              >
                <Input placeholder="请输入权限编码" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="权限类型"
                name="permType"
                rules={[{ required: true, message: '请选择权限类型' }]}
              >
                <Select placeholder="请选择权限类型">
                  <Option value={1}>菜单</Option>
                  <Option value={2}>按钮</Option>
                  <Option value={3}>API</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="上级权限"
                name="parentId"
              >
                <TreeSelect
                  placeholder="请选择上级权限"
                  treeData={permissionTree}
                  treeDefaultExpandAll
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.permType !== currentValues.permType}
          >
            {({ getFieldValue }) => {
              const permType = getFieldValue('permType');
              return permType === 1 ? (
                <Form.Item
                  label="路径"
                  name="permPath"
                  rules={[
                    { required: true, message: '菜单类型必须填写路径' },
                    { pattern: /^\/.+/, message: '路径必须以/开头' }
                  ]}
                >
                  <Input placeholder="请输入路径，如：/users" />
                </Form.Item>
              ) : null;
            }}
          </Form.Item>

          <Form.Item
            label="排序"
            name="sortOrder"
          >
            <Input type="number" placeholder="请输入排序号" />
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
    </div>
  );
};

export default PermissionManagement; 
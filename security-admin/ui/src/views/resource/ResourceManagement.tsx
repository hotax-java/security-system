import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Input,
  Form,
  message,
  Card,
  Row,
  Col,
  Select,
  InputNumber,
  Switch,
  Popconfirm
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined
} from '@ant-design/icons';
import resourceService from '../../apis/resourceService';
import { PagedDTO, ResourceVO, ResourceDTO } from '../../apis/types';

const { Option } = Select;

const ResourceManagement: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [resources, setResources] = useState<ResourceVO[]>([]);
  const [total, setTotal] = useState<number>(0);
  const [current, setCurrent] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);
  const [keyword, setKeyword] = useState<string>('');
  const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
  const [editingResource, setEditingResource] = useState<ResourceVO | null>(null);
  const [form] = Form.useForm();

  // 加载资源列表
  const loadResources = async (page: number = current, size: number = pageSize, search: string = keyword) => {
    setLoading(true);
    try {
      const pagedDTO: PagedDTO = {
        pageNum: page,
        pageSize: size,
        keyword: search
      };
      const response = await resourceService.paged(pagedDTO);
      if (response && typeof response === 'object') {
        if ('records' in response && Array.isArray(response.records)) {
      setResources(response.records);
          setTotal(response.total || 0);
        } else if ('list' in response && Array.isArray(response.list)) {
          setResources(response.list);
          setTotal(response.total || 0);
        } else if (Array.isArray(response)) {
          setResources(response);
          setTotal(response.length);
        } else if ('data' in response && typeof response.data === 'object') {
          // 处理嵌套在data字段中的情况
          const data = response.data as any;
          if ('records' in data && Array.isArray(data.records)) {
            setResources(data.records);
            setTotal(data.total || 0);
          } else if ('list' in data && Array.isArray(data.list)) {
            setResources(data.list);
            setTotal(data.total || 0);
          } else if (Array.isArray(response.data)) {
            setResources(response.data);
            setTotal(response.data.length || 0);
          } else {
            console.error('无法识别的响应数据格式:', response);
            setResources([]);
            setTotal(0);
          }
        } else {
          console.error('无法识别的响应数据格式:', response);
          setResources([]);
          setTotal(0);
        }
      } else {
        console.error('响应数据无效:', response);
        setResources([]);
        setTotal(0);
      }
    } catch (error) {
      console.error('加载API资源失败:', error);
      message.error('加载数据失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadResources();
  }, []);

  // 处理表格分页变化
  const handleTableChange = (pagination: any) => {
    setCurrent(pagination.current);
    setPageSize(pagination.pageSize);
    loadResources(pagination.current, pagination.pageSize);
  };

  // 处理搜索
  const handleSearch = () => {
    setCurrent(1);
    loadResources(1, pageSize, keyword);
  };

  // 打开创建/编辑模态框
  const showModal = (record?: ResourceVO) => {
    form.resetFields();
    if (record) {
      setEditingResource(record);
      form.setFieldsValue({
        resourceCode: record.resourceCode,
        resourceName: record.resourceName,
        resourcePath: record.resourcePath,
        method: record.method,
        qpsLimit: record.qpsLimit,
        burstCapacity: record.burstCapacity,
        dailyQuota: record.dailyQuota,
        concurrencyLimit: record.concurrencyLimit,
        status: record.status
      });
    } else {
      setEditingResource(null);
      form.setFieldsValue({
        status: 1
      });
    }
    setIsModalVisible(true);
  };

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (editingResource) {
        // 更新资源
        await resourceService.update(editingResource.resourceId, values as ResourceDTO);
        message.success('更新成功');
      } else {
        // 创建资源
        await resourceService.create(values as ResourceDTO);
        message.success('创建成功');
      }

      setIsModalVisible(false);
      loadResources();
    } catch (error) {
      console.error('保存API资源失败:', error);
      message.error('操作失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 处理删除资源
  const handleDelete = async (id: number) => {
    try {
      setLoading(true);
      await resourceService.remove(id);
      message.success('删除成功');
      loadResources();
    } catch (error) {
      console.error('删除API资源失败:', error);
      message.error('删除失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '资源编码',
      dataIndex: 'resourceCode',
      key: 'resourceCode',
    },
    {
      title: '资源名称',
      dataIndex: 'resourceName',
      key: 'resourceName',
    },
    {
      title: '接口路径',
      dataIndex: 'resourcePath',
      key: 'resourcePath',
      ellipsis: true,
    },
    {
      title: '请求方法',
      dataIndex: 'method',
      key: 'method',
    },
    {
      title: 'QPS限制',
      dataIndex: 'qpsLimit',
      key: 'qpsLimit',
      render: (qpsLimit?: number) => qpsLimit || '无限制',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Switch
          checkedChildren="启用"
          unCheckedChildren="禁用"
          checked={status === 1}
          disabled
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: ResourceVO) => (
        <Space size="middle">
          <Button
            icon={<EditOutlined />}
            onClick={() => showModal(record)}
            size="small"
            type="primary"
            ghost
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除该资源吗？"
            onConfirm={() => handleDelete(record.resourceId)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              icon={<DeleteOutlined />}
              size="small"
              danger
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title="API资源管理" extra={
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={() => showModal()}
        >
          新增资源
        </Button>
      }>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input
              placeholder="搜索资源编码、名称或路径"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={handleSearch}
              suffix={
                <SearchOutlined onClick={handleSearch} style={{ cursor: 'pointer' }} />
              }
            />
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={resources}
          rowKey="resourceId"
          loading={loading}
          pagination={{
            current,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          onChange={handleTableChange}
        />
      </Card>

      {/* 创建/编辑资源表单 */}
      <Modal
        title={editingResource ? '编辑API资源' : '创建API资源'}
        open={isModalVisible}
        onOk={handleSubmit}
        onCancel={() => setIsModalVisible(false)}
        okText="确认"
        cancelText="取消"
        confirmLoading={loading}
        width={700}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="resourceCode"
                label="资源编码"
                rules={[{ required: true, message: '请输入资源编码' }]}
              >
                <Input placeholder="如: user:query" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="resourceName"
                label="资源名称"
                rules={[{ required: true, message: '请输入资源名称' }]}
              >
                <Input placeholder="如: 用户查询接口" />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={16}>
              <Form.Item
                name="resourcePath"
                label="接口路径"
                rules={[{ required: true, message: '请输入接口路径' }]}
              >
                <Input placeholder="如: /api/v1/users/**" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="method"
                label="请求方法"
                rules={[{ required: true, message: '请选择请求方法' }]}
              >
                <Select placeholder="请选择">
                  <Option value="GET">GET</Option>
                  <Option value="POST">POST</Option>
                  <Option value="PUT">PUT</Option>
                  <Option value="DELETE">DELETE</Option>
                  <Option value="*">*（全部）</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item
                name="qpsLimit"
                label="QPS限制"
                tooltip="每秒请求上限，不填表示不限制"
              >
                <InputNumber min={0} placeholder="不限制" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="burstCapacity"
                label="突发容量"
                tooltip="令牌桶突发容量，不填表示不限制"
              >
                <InputNumber min={0} placeholder="不限制" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="dailyQuota"
                label="日调用上限"
                tooltip="每日调用上限，不填表示不限制"
              >
                <InputNumber min={0} placeholder="不限制" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="concurrencyLimit"
                label="并发限制"
                tooltip="最大并发请求数，不填表示不限制"
              >
                <InputNumber min={0} placeholder="不限制" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          
          <Form.Item
            name="status"
            label="状态"
            valuePropName="checked"
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ResourceManagement; 
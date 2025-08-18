import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Input,
  Form,
  message,
  Tag,
  Typography,
  Popconfirm,
  Card,
  Row,
  Col,
  Alert,
  Tooltip,
  Switch,
  Transfer,
  Spin
} from 'antd';
import {
  PlusOutlined,
  DownloadOutlined,
  SearchOutlined,
  CopyOutlined,
  KeyOutlined,
  LockOutlined,
  UnlockOutlined,
  ApiOutlined
} from '@ant-design/icons';
import credentialService from '../../apis/credentialService';
import resourceService from '../../apis/resourceService';
import { PagedDTO, CredentialVO, CredentialCreateResultDTO, ResourceVO } from '../../apis/types';
import type { TransferDirection } from 'antd/es/transfer';
import type { Key } from 'react';

const { Text, Paragraph } = Typography;

// 将资源对象转换为Transfer组件所需的数据结构
interface TransferItem {
  key: string;
  title: string;
  description: string;
  disabled: boolean;
}

const CredentialManagement: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [credentials, setCredentials] = useState<CredentialVO[]>([]);
  const [total, setTotal] = useState<number>(0);
  const [current, setCurrent] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);
  const [keyword, setKeyword] = useState<string>('');
  const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
  const [createResultVisible, setCreateResultVisible] = useState<boolean>(false);
  const [createResult, setCreateResult] = useState<CredentialCreateResultDTO | null>(null);
  // 新增的状态变量
  const [assignModalVisible, setAssignModalVisible] = useState<boolean>(false);
  const [selectedCredential, setSelectedCredential] = useState<number | null>(null);
  const [selectedCredentialName, setSelectedCredentialName] = useState<string>('');
  const [resources, setResources] = useState<ResourceVO[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<Key[]>([]);
  const [transferLoading, setTransferLoading] = useState<boolean>(false);
  const [form] = Form.useForm();

  // 加载凭证列表
  const loadCredentials = async (page: number = current, size: number = pageSize, search: string = keyword) => {
    setLoading(true);
    try {
      const pagedDTO: PagedDTO = {
        pageNum: page,
        pageSize: size,
        keyword: search
      };
      const response = await credentialService.paged(pagedDTO);
      console.log('API返回数据:', response);
      
      // 处理返回结果，适应不同的响应格式
      if (response && typeof response === 'object') {
        // 如果返回的是分页对象，可能有records字段
        if ('records' in response && Array.isArray(response.records)) {
          setCredentials(response.records);
          setTotal(response.total || 0);
        }
        // 如果返回的是分页对象，可能有list字段 (参考UserManagement)
        else if ('list' in response && Array.isArray(response.list)) {
          setCredentials(response.list);
          setTotal(response.total || 0);
        }
        // 如果返回的是数组
        else if (Array.isArray(response)) {
          setCredentials(response);
          setTotal(response.length);
        } else {
          console.warn('API返回的数据格式不符合预期:', response);
          setCredentials([]);
          setTotal(0);
        }
      } else {
        console.warn('API响应无效:', response);
        setCredentials([]);
        setTotal(0);
      }
    } catch (error) {
      console.error('加载客户端凭证失败:', error);
      message.error('加载数据失败，请重试');
      setCredentials([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCredentials();
  }, []);

  // 处理表格分页变化
  const handleTableChange = (pagination: any) => {
    setCurrent(pagination.current);
    setPageSize(pagination.pageSize);
    loadCredentials(pagination.current, pagination.pageSize);
  };

  // 处理搜索
  const handleSearch = () => {
    setCurrent(1);
    loadCredentials(1, pageSize, keyword);
  };

  // 预生成凭证信息
  const [tempCredential, setTempCredential] = useState<{appId?: string, appSecret?: string} | null>(null);
  
  // 生成临时凭证信息
  const generateTempCredential = async () => {
    try {
      setLoading(true);
      // 新增API: 调用后端API生成临时凭证
      const response = await credentialService.generateCredential();
      console.log('预生成凭证响应:', response);
      
      if (!response || !response.appId || !response.appSecret) {
        throw new Error('生成凭证信息失败');
      }
      
      setTempCredential(response);
    } catch (error) {
      console.error('生成临时凭证失败:', error);
      message.error('生成凭证信息失败，请重试');
    } finally {
      setLoading(false);
    }
  };
  
  // 打开创建凭证对话框
  const showCreateModal = async () => {
    form.resetFields();
    setTempCredential(null);
    await generateTempCredential();
    setIsModalVisible(true);
  };
  
  // 处理创建凭证
  const handleCreate = async () => {
    try {
      if (!tempCredential) {
        throw new Error('凭证信息未生成，请刷新重试');
      }
      
      const values = await form.validateFields();
      setLoading(true);
      
      // 允许备注为空，使用空字符串作为默认值
      const remark = values.remark || '';
      
      // 使用预生成的凭证信息和备注保存
      if (!tempCredential.appId || !tempCredential.appSecret) {
        throw new Error('凭证信息不完整');
      }
      
      const response = await credentialService.saveCredential({
        appId: tempCredential.appId,
        appSecret: tempCredential.appSecret,
        remark: remark
      });
      
      console.log('保存凭证响应:', response);
      
      // 保存结果并显示
      setCreateResult({
        appId: tempCredential.appId || '',
        appSecret: tempCredential.appSecret || ''
      });
      setIsModalVisible(false);
      setCreateResultVisible(true);
      form.resetFields();
      loadCredentials();
      message.success('创建成功');
    } catch (error) {
      console.error('创建凭证失败:', error);
      message.error(error instanceof Error ? error.message : '创建失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 处理前端下载凭证
  const handleDownload = (appId: string, plainSecret: string | null = null) => {
    try {
      const secretToUse = plainSecret || "<请在创建完成后立即下载获取明文密钥，后续无法再次获取>";
      const content = `appid=${appId}\nappsecret=${secretToUse}\n`;
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `credential-${appId}.txt`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('下载凭证失败:', error);
      message.error('下载失败，请重试');
    }
  };

  // 处理状态切换
  const handleStatusChange = async (record: CredentialVO, checked: boolean) => {
    try {
      const status = checked ? 1 : 0;
      await credentialService.updateStatus(record.appId, status);
      message.success(`${checked ? '启用' : '禁用'}成功`);
      loadCredentials();
    } catch (error) {
      console.error('更新状态失败:', error);
      message.error('操作失败，请重试');
    }
  };

  // 复制到剪贴板
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(
      () => {
        message.success('已复制到剪贴板');
      },
      () => {
        message.error('复制失败，请手动复制');
      }
    );
  };

  // 新增函数 - 打开API权限分配对话框
  const showAssignModal = async (record: CredentialVO) => {
    setSelectedCredential(record.id);
    setSelectedCredentialName(record.appId);
    setAssignModalVisible(true);
    await loadResources();
    await loadAssignedResources(record.id);
  };

  // 新增函数 - 加载所有API资源
  const loadResources = async () => {
    try {
      setTransferLoading(true);
      const response = await resourceService.all();
      setResources(response);
    } catch (error) {
      console.error('加载API资源失败:', error);
      message.error('加载API资源失败，请重试');
    } finally {
      setTransferLoading(false);
    }
  };

  // 新增函数 - 加载已分配的资源ID
  const loadAssignedResources = async (credentialId: number) => {
    try {
      setTransferLoading(true);
      const response = await credentialService.getResourceIds(credentialId);
      setSelectedKeys(response.map((id: number) => id.toString()));
    } catch (error) {
      console.error('加载已授权资源失败:', error);
      message.error('加载已授权资源失败，请重试');
      setSelectedKeys([]);
    } finally {
      setTransferLoading(false);
    }
  };

  // 新增函数 - 处理Transfer变更
  const handleTransferChange = (nextTargetKeys: Key[], direction: TransferDirection, moveKeys: Key[]) => {
    setSelectedKeys(nextTargetKeys);
  };

  // 新增函数 - 保存权限分配
  const handleSaveAssignment = async () => {
    if (!selectedCredential) {
      message.warning('请先选择客户端凭证');
      return;
    }

    try {
      setTransferLoading(true);
      const resourceIds = selectedKeys.map(key => parseInt(key.toString()));
      await credentialService.assignResources(selectedCredential, resourceIds);
      message.success('权限分配成功');
      setAssignModalVisible(false);
    } catch (error) {
      console.error('保存权限分配失败:', error);
      message.error('保存失败，请重试');
    } finally {
      setTransferLoading(false);
    }
  };

  // 新增函数 - 将资源列表转换为Transfer数据源
  const getTransferItems = (): TransferItem[] => {
    return resources.map(resource => ({
      key: resource.resourceId.toString(),
      title: resource.resourceName,
      description: `${resource.method} ${resource.resourcePath} (${resource.resourceCode})`,
      disabled: resource.status !== 1
    }));
  };

  const columns = [
    {
      title: 'AppID',
      dataIndex: 'appId',
      key: 'appId',
      render: (text: string) => (
        <Tooltip title="点击复制">
          <Typography.Text copyable={{ text, tooltips: ['复制', '已复制'] }}>
            {text.substring(0, 8)}...{text.substring(text.length - 4)}
          </Typography.Text>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number, record: CredentialVO) => (
        <Switch
          checkedChildren="启用"
          unCheckedChildren="禁用"
          checked={status === 1}
          onChange={(checked) => handleStatusChange(record, checked)}
        />
      ),
    },
    {
      title: '创建者',
      dataIndex: 'createBy',
      key: 'createBy',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: CredentialVO) => (
        <Space size="middle">
          <Button
            icon={<ApiOutlined />}
            onClick={() => showAssignModal(record)}
            size="small"
            type="primary"
            ghost
          >
            分配API权限
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title="客户端凭证管理" extra={
        <Button 
          type="primary" 
          icon={<PlusOutlined />} 
          onClick={showCreateModal}
        >
          新增凭证
        </Button>
      }>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input
              placeholder="搜索AppID、创建者或备注"
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
          dataSource={credentials}
          rowKey="id"
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

      {/* 创建凭证表单 */}
      <Modal
        title="创建客户端凭证"
        open={isModalVisible}
        onOk={handleCreate}
        onCancel={() => setIsModalVisible(false)}
        okText="确认保存"
        cancelText="取消"
        confirmLoading={loading}
        width={700}
      >
        {loading && !tempCredential ? (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <Spin tip="正在生成凭证信息..." />
          </div>
        ) : (
          <Form form={form} layout="vertical">
            {tempCredential && (
              <>
                <Alert
                  message="重要提示"
                  description="请记录AppID和AppSecret，点击确认后将保存凭证信息。"
                  type="warning"
                  showIcon
                  style={{ marginBottom: 16 }}
                />
                
                <Form.Item label="AppID">
                  <div style={{ 
                    background: '#f5f5f5', 
                    padding: '12px 16px', 
                    borderRadius: '4px',
                    position: 'relative'
                  }}>
                    <Text style={{ fontSize: 16, wordBreak: 'break-all', display: 'block' }}>
                      {tempCredential.appId}
                    </Text>
                                         <Button 
                      icon={<CopyOutlined />} 
                      type="text"
                      onClick={() => tempCredential.appId && copyToClipboard(tempCredential.appId)}
                      style={{ position: 'absolute', right: 8, top: 8 }}
                    />
                  </div>
                </Form.Item>
                
                <Form.Item label="AppSecret">
                  <div style={{ 
                    background: '#fffbe6', 
                    padding: '12px 16px', 
                    borderRadius: '4px',
                    border: '1px solid #ffe58f',
                    position: 'relative'
                  }}>
                    <Text style={{ fontSize: 16, wordBreak: 'break-all', display: 'block', fontWeight: 'bold' }}>
                      {tempCredential.appSecret}
                    </Text>
                                         <Button 
                      icon={<CopyOutlined />} 
                      type="text"
                      onClick={() => tempCredential.appSecret && copyToClipboard(tempCredential.appSecret)}
                      style={{ position: 'absolute', right: 8, top: 8 }}
                    />
                  </div>
                </Form.Item>
              </>
            )}
            
            <Form.Item
              name="remark"
              label="备注"
              rules={[{ required: false }]}
            >
              <Input placeholder="请输入备注信息，如：用途、负责人等（选填）" />
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 创建结果展示 */}
      <Modal
        title={<><KeyOutlined /> 凭证信息</>}
        open={createResultVisible}
        onCancel={() => setCreateResultVisible(false)}
        footer={[
          <Button 
            key="download" 
            type="primary" 
            icon={<DownloadOutlined />}
            onClick={() => {
              if (createResult && createResult.appId) {
                handleDownload(createResult.appId, createResult.appSecret || null);
              }
            }}
          >
            下载凭证
          </Button>,
          <Button 
            key="copy"
            type="primary"
            icon={<CopyOutlined />}
            onClick={() => {
              if (createResult && createResult.appId && createResult.appSecret) {
                const textToCopy = 
                `AppID: ${createResult.appId}\nAppSecret: ${createResult.appSecret}`;
                copyToClipboard(textToCopy);
              }
            }}
          >
            复制全部
          </Button>,
          <Button 
            key="close" 
            onClick={() => setCreateResultVisible(false)}
          >
            关闭
          </Button>
        ]}
        width={700}
      >
        <Alert
          message="重要提示"
          description="AppSecret 仅显示一次，请立即复制或下载！关闭此窗口后将无法再次查看完整的密钥。"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
        
        {createResult && (
          <Card>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Text strong style={{ fontSize: 16 }}>AppID:</Text>
                <div 
                  style={{ 
                    background: '#f5f5f5', 
                    padding: '12px 16px', 
                    borderRadius: '4px',
                    marginTop: 8,
                    marginBottom: 4,
                    position: 'relative'
                  }}
                >
                  <Text style={{ fontSize: 16, wordBreak: 'break-all', display: 'block' }}>
                    {createResult.appId}
                  </Text>
                  <Button 
                    icon={<CopyOutlined />} 
                    type="text"
                                          onClick={() => createResult.appId && copyToClipboard(createResult.appId)}
                    style={{ position: 'absolute', right: 8, top: 8 }}
                  />
                </div>
              </Col>
              <Col span={24}>
                <Text strong style={{ fontSize: 16 }}>AppSecret:</Text>
                <div 
                  style={{ 
                    background: '#fffbe6', 
                    padding: '12px 16px', 
                    borderRadius: '4px',
                    border: '1px solid #ffe58f',
                    marginTop: 8,
                    marginBottom: 4,
                    position: 'relative'
                  }}
                >
                  <Text style={{ fontSize: 16, wordBreak: 'break-all', display: 'block', fontWeight: 'bold' }}>
                    {createResult.appSecret}
                  </Text>
                  <Button 
                    icon={<CopyOutlined />} 
                    type="text"
                                          onClick={() => createResult.appSecret && copyToClipboard(createResult.appSecret)}
                    style={{ position: 'absolute', right: 8, top: 8 }}
                  />
                </div>
              </Col>
            </Row>
          </Card>
        )}
      </Modal>

      {/* 新增 - API权限分配模态框 */}
      <Modal
        title={<><ApiOutlined /> API权限分配 - {selectedCredentialName}</>}
        open={assignModalVisible}
        onOk={handleSaveAssignment}
        onCancel={() => setAssignModalVisible(false)}
        okText="确认"
        cancelText="取消"
        width={800}
        confirmLoading={transferLoading}
        maskClosable={false}
      >
        <Spin spinning={transferLoading}>
          <Alert
            message="权限提示"
            description="API权限更改将在下次客户端获取新token时生效。已授权的token在过期前仍然有效。"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Transfer
            dataSource={getTransferItems()}
            titles={['可用API资源', '已授权API资源']}
            targetKeys={selectedKeys}
            onChange={handleTransferChange}
            render={item => (
              <div>
                <div>{item.title}</div>
                <div style={{ fontSize: '12px', color: '#999' }}>{item.description}</div>
              </div>
            )}
            listStyle={{
              width: '45%',
              height: 500,
            }}
            showSearch
            filterOption={(inputValue, item) =>
              item.title.indexOf(inputValue) !== -1 || 
              item.description.indexOf(inputValue) !== -1
            }
          />
        </Spin>
      </Modal>
    </div>
  );
};

export default CredentialManagement; 
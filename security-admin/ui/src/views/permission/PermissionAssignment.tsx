import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  message,
  Card,
  Row,
  Col,
  Select,
  Transfer,
  Typography,
  Spin
} from 'antd';
import { ApiOutlined, KeyOutlined } from '@ant-design/icons';
import credentialService from '../../apis/credentialService';
import resourceService from '../../apis/resourceService';
import { CredentialVO, ResourceVO } from '../../apis/types';
import { TransferDirection } from 'antd/es/transfer';
import type { Key } from 'react';

const { Title, Text } = Typography;
const { Option } = Select;

interface TransferItem {
  key: string;
  title: string;
  description: string;
  disabled: boolean;
}

const PermissionAssignment: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [credentials, setCredentials] = useState<CredentialVO[]>([]);
  const [resources, setResources] = useState<ResourceVO[]>([]);
  const [selectedCredential, setSelectedCredential] = useState<number | null>(null);
  const [selectedKeys, setSelectedKeys] = useState<Key[]>([]);
  const [transferLoading, setTransferLoading] = useState<boolean>(false);
  const [saveLoading, setSaveLoading] = useState<boolean>(false);

  // 加载客户端凭证列表
  const loadCredentials = async () => {
    try {
      setLoading(true);
      const response = await credentialService.all();
      setCredentials(response);
    } catch (error) {
      console.error('加载客户端凭证失败:', error);
      message.error('加载客户端凭证失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 加载API资源列表
  const loadResources = async () => {
    try {
      setLoading(true);
      const response = await resourceService.all();
      setResources(response);
    } catch (error) {
      console.error('加载API资源失败:', error);
      message.error('加载API资源失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 加载已授权资源ID列表
  const loadAuthorizedResources = async (credentialId: number) => {
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

  // 初始化加载
  useEffect(() => {
    Promise.all([loadCredentials(), loadResources()]);
  }, []);

  // 处理凭证选择变更
  const handleCredentialChange = (value: number) => {
    setSelectedCredential(value);
    if (value) {
      loadAuthorizedResources(value);
    } else {
      setSelectedKeys([]);
    }
  };

  // 处理Transfer变更
  const handleTransferChange = (targetKeys: Key[], direction: TransferDirection, moveKeys: Key[]) => {
    setSelectedKeys(targetKeys);
  };

  // 保存权限分配
  const handleSave = async () => {
    if (!selectedCredential) {
      message.warning('请先选择客户端凭证');
      return;
    }

    try {
      setSaveLoading(true);
      const resourceIds = selectedKeys.map(key => parseInt(key.toString()));
      await credentialService.assignResources(selectedCredential, resourceIds);
      message.success('权限分配成功');
    } catch (error) {
      console.error('保存权限分配失败:', error);
      message.error('保存失败，请重试');
    } finally {
      setSaveLoading(false);
    }
  };

  // 将资源列表转换为Transfer数据源
  const getTransferItems = (): TransferItem[] => {
    return resources.map(resource => ({
      key: resource.resourceId.toString(),
      title: resource.resourceName,
      description: `${resource.method} ${resource.resourcePath} (${resource.resourceCode})`,
      disabled: resource.status !== 1
    }));
  };

  return (
    <div>
      <Card title="API权限分配" extra={
        <Button 
          type="primary" 
          onClick={handleSave}
          loading={saveLoading}
          disabled={!selectedCredential}
        >
          保存权限配置
        </Button>
      }>
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={12}>
            <Text strong>选择客户端凭证：</Text>
            <Select
              placeholder="请选择客户端凭证"
              style={{ width: '100%', marginTop: 8 }}
              onChange={handleCredentialChange}
              loading={loading}
              allowClear
            >
              {credentials.map(cred => (
                <Option key={cred.id} value={cred.id}>
                  <Space>
                    <KeyOutlined />
                    <span>{cred.appId}</span>
                    {cred.remark && <Text type="secondary">({cred.remark})</Text>}
                    {cred.status !== 1 && <Text type="danger">[已禁用]</Text>}
                  </Space>
                </Option>
              ))}
            </Select>
          </Col>
        </Row>

        <Spin spinning={transferLoading}>
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
            disabled={!selectedCredential}
            showSearch
            filterOption={(inputValue, item) =>
              item.title.indexOf(inputValue) !== -1 || 
              item.description.indexOf(inputValue) !== -1
            }
          />
        </Spin>

        <Row style={{ marginTop: 16 }}>
          <Col span={24}>
            <Text type="secondary">
              注意：权限更改将在下次客户端获取新token时生效。已授权的token在过期前仍然有效。
            </Text>
          </Col>
        </Row>
      </Card>
    </div>
  );
};

export default PermissionAssignment; 
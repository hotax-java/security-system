import React, { useState, useEffect } from 'react';
import {
  Layout,
  Menu,
  Button,
  Avatar,
  Dropdown,
  Space,
  Breadcrumb,
  Card,
  Row,
  Col,
  Statistic,
  message
} from 'antd';
import {
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  SettingOutlined,
  MenuOutlined,
  ApiOutlined,
  KeyOutlined,
  AppstoreOutlined
} from '@ant-design/icons';
import UserManagement from '../../views/user/UserManagement';
import RoleManagement from '../../views/role/RoleManagement';
import PermissionManagement from '../../views/permission/PermissionManagement';
import CredentialManagement from '../../views/credential/CredentialManagement';
import ResourceManagement from '../../views/resource/ResourceManagement';
import PermissionAssignment from '../../views/permission/PermissionAssignment';
import ProtectedRoute from '../../components/common/ProtectedRoute';
import { authService } from '../../services/authService';
import { MenuService, MenuItem, DashboardStats } from '../../apis/menuService';
import { TokenManager } from '../../services/tokenManager';

const { Header, Sider, Content } = Layout;

interface DashboardProps {
  user: any;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const [userInfo, setUserInfo] = useState<any>(null);
  const [menuItems, setMenuItems] = useState<any[]>([]);
  const [dashboardStats, setDashboardStats] = useState<DashboardStats>({
    totalUsers: 0,
    activeUsers: 0,
    onlineUsers: 0,
    totalRoles: 0,
    totalPermissions: 0,
    recentUsers: []
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 设置用户信息
    if (user) {
      setUserInfo(user);
    } else {
      // 如果没有通过props传递，则尝试从TokenManager获取
      const userData = TokenManager.getUserInfo();
      if (userData) {
        setUserInfo(userData);
      }
    }

    // 加载菜单和统计数据
    loadDashboardData();
  }, [user]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // 并行加载菜单和统计数据
      const [menus, stats] = await Promise.all([
        MenuService.getMenus(), // 获取当前用户的菜单权限
        MenuService.getDashboardStats()
      ]);

      // 确保menus是数组类型
      const menuArray = Array.isArray(menus) ? menus : [];
      console.log('加载的菜单数据:', menuArray.length, '个菜单项');
      
      // 过滤有权限的菜单
      const filteredMenus = MenuService.filterMenusByPermission(menuArray);
      
      // 转换为Ant Design Menu格式
      const antdMenuItems = MenuService.convertToAntdMenuItems(filteredMenus);
      
      // 添加默认的Dashboard菜单
      const defaultMenuItems = [
        {
          key: 'dashboard',
          label: '仪表盘',
          icon: <DashboardOutlined />
        },
        ...antdMenuItems
      ];
      
      setMenuItems(defaultMenuItems);
      setDashboardStats(stats);
    } catch (error) {
      console.error('加载仪表盘数据失败:', error);
      message.error('加载数据失败，请刷新页面重试');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await authService.logout();
      onLogout();
    } catch (error) {
      console.error('Logout failed:', error);
      // 即使登出失败，也清除本地存储并调用onLogout
      TokenManager.clearTokens();
      onLogout();
    }
  };

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  const getBreadcrumbItems = () => {
    const breadcrumbMap: { [key: string]: string[] } = {
      'dashboard': ['仪表盘'],
      'USER_MANAGE': ['系统管理', '用户管理'],
      'ROLE_MANAGE': ['系统管理', '角色管理'],
      'PERMISSION_MANAGE': ['系统管理', '权限管理'],
      'CLIENT_CREDENTIAL_MANAGE': ['OpenAPI管理', '客户端凭证'],
      'API_RESOURCE_MANAGE': ['OpenAPI管理', 'API资源'],
      'API_PERMISSION_ASSIGN': ['OpenAPI管理', 'API权限分配'],
    };
    
    return breadcrumbMap[selectedKey] || ['仪表盘'];
  };

  const renderContent = () => {
    switch (selectedKey) {
      case 'USER_MANAGE':
        return (
          <ProtectedRoute permission="USER_MANAGE">
            <UserManagement />
          </ProtectedRoute>
        );
      case 'ROLE_MANAGE':
        return (
          <ProtectedRoute permission="ROLE_MANAGE">
            <RoleManagement />
          </ProtectedRoute>
        );
      case 'PERMISSION_MANAGE':
        return (
          <ProtectedRoute permission="PERMISSION_MANAGE">
            <PermissionManagement />
          </ProtectedRoute>
        );
      case 'CLIENT_CREDENTIAL_MANAGE':
      case 'OPENAPI_CREDENTIAL':
        return (
          <ProtectedRoute permission="OPENAPI_CREDENTIAL_QUERY">
            <CredentialManagement />
          </ProtectedRoute>
        );
      case 'API_RESOURCE_MANAGE':
      case 'OPENAPI_RESOURCE':
        return (
          <ProtectedRoute permission="OPENAPI_RESOURCE_QUERY">
            <ResourceManagement />
          </ProtectedRoute>
        );
      case 'API_PERMISSION_ASSIGN':
      case 'OPENAPI_PERMISSION':
        return (
          <ProtectedRoute permission="OPENAPI_PERMISSION_QUERY">
            <PermissionAssignment />
          </ProtectedRoute>
        );
      default:
        return (
          <div style={{ padding: 24 }}>
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总用户数"
                    value={dashboardStats.totalUsers}
                    prefix={<UserOutlined />}
                    valueStyle={{ color: '#3f8600' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总角色数"
                    value={dashboardStats.totalRoles}
                    prefix={<TeamOutlined />}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总权限数"
                    value={dashboardStats.totalPermissions}
                    prefix={<SafetyOutlined />}
                    valueStyle={{ color: '#722ed1' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="在线用户"
                    value={dashboardStats.onlineUsers}
                    prefix={<UserOutlined />}
                    valueStyle={{ color: '#cf1322' }}
                  />
                </Card>
              </Col>
            </Row>
            
            <Card title="系统概览" style={{ marginBottom: 24 }}>
              <p>欢迎使用权限管理系统！</p>
              <p>这是一个基于 Spring Boot + React + Ant Design 构建的权限管理系统。</p>
              <p>您可以通过左侧菜单进行用户、角色、权限的管理操作。</p>
            </Card>

            <Card title="快速操作">
              <Space>
                {menuItems.filter(item => 
                  item.key === 'USER_MANAGE' || 
                  item.key === 'ROLE_MANAGE' || 
                  item.key === 'PERMISSION_MANAGE' ||
                  item.key === 'CLIENT_CREDENTIAL_MANAGE' ||
                  item.key === 'API_RESOURCE_MANAGE' ||
                  item.key === 'API_PERMISSION_ASSIGN'
                ).map(item => (
                  <Button 
                    key={item.key}
                    type="primary" 
                    icon={item.icon} 
                    onClick={() => setSelectedKey(item.key)}
                  >
                    {item.label}
                  </Button>
                ))}
              </Space>
            </Card>
          </div>
        );
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed} 
        theme="light" 
        width={220}
        style={{ boxShadow: '2px 0 8px 0 rgba(29,35,41,.05)' }}
      >
        {/* 系统Logo和名称 */}
        <div style={{ 
          padding: collapsed ? '22px 0' : '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: collapsed ? 'center' : 'flex-start',
          backgroundColor: '#ffffff',
          height: 64,
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
          margin: '0 0 1px 0'
        }}>
          <div style={{ 
            background: 'linear-gradient(135deg, #1890ff 0%, #722ed1 100%)', 
            width: 36, 
            height: 36, 
            borderRadius: '8px', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            marginRight: collapsed ? 0 : 12
          }}>
            <SafetyOutlined style={{ color: 'white', fontSize: 20 }} />
          </div>
          {!collapsed && (
            <div>
              <div style={{ 
                color: '#000000', 
                fontSize: 16, 
                fontWeight: 'bold', 
                lineHeight: '20px',
                letterSpacing: '0.5px'
              }}>
                安全权限系统
              </div>
              <div style={{ color: 'rgba(0,0,0,0.65)', fontSize: 12, marginTop: 2 }}>
                Security Admin
              </div>
            </div>
          )}
        </div>

        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={[]}
          items={loading ? [] : menuItems}
          onClick={({ key }) => setSelectedKey(key)}
          style={{ 
            borderRight: 0, 
            padding: '8px 0',
            fontSize: '14px'
          }}
        />
      </Sider>
      
      <Layout>
        <Header style={{ 
          padding: '0 24px', 
          background: '#fff', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          zIndex: 10,
          position: 'sticky',
          top: 0
        }}>
          <Space>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: '16px' }}
            />
            <Breadcrumb style={{ marginLeft: 12 }}>
              {getBreadcrumbItems().map((item, index) => (
                <Breadcrumb.Item key={index}>{item}</Breadcrumb.Item>
              ))}
            </Breadcrumb>
          </Space>
          
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
          >
            <Space style={{ cursor: 'pointer' }}>
              <Avatar 
                style={{ 
                  backgroundColor: '#1890ff',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)'
                }} 
                icon={<UserOutlined />} 
              />
              <span style={{ fontSize: '14px' }}>{userInfo?.username || '管理员'}</span>
            </Space>
          </Dropdown>
        </Header>
        
        <Content style={{ 
          margin: '16px', 
          minHeight: 280,
          background: '#f0f2f5'
        }}>
          <div style={{ 
            padding: '24px', 
            background: '#fff', 
            borderRadius: '4px',
            minHeight: 'calc(100vh - 140px)',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)'
          }}>
            {renderContent()}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default Dashboard; 
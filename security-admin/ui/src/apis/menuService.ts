import React from 'react';
import businessApi from './api';
import { PermissionUtil } from '../utils/permissionUtil';
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  SettingOutlined,
  MenuOutlined,
  ApiOutlined,
  KeyOutlined,
  AppstoreOutlined
} from '@ant-design/icons';

export interface MenuItem {
  permissionId: number;
  permCode: string;
  permName: string;
  description?: string;
  permType: number;
  parentId?: number;
  permPath?: string;
  icon?: string; // 数据库中的图标
  status: number;
  sortOrder: number;
  children?: MenuItem[];
}

export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  onlineUsers: number;
  totalRoles: number;
  totalPermissions: number;
  recentUsers: any[];
}

export interface AntdMenuItem {
  key: string;
  label: string;
  icon?: any;
  children?: AntdMenuItem[];
}

/**
 * 菜单服务
 */
export class MenuService {
  /**
   * 获取当前用户的菜单列表
   */
  static async getMenus(): Promise<MenuItem[]> {
    try {
      const response = await businessApi.get('/api/dashboard/menus');
      return (response as unknown as MenuItem[]) || [];
    } catch (error) {
      console.error('获取菜单失败:', error);
      return [];
    }
  }

  /**
   * 获取仪表盘统计数据
   */
  static async getDashboardStats(): Promise<DashboardStats> {
    try {
      const response = await businessApi.get('/api/dashboard/stats');
      return (response as unknown as DashboardStats) || {
        totalUsers: 0,
        activeUsers: 0,
        onlineUsers: 0,
        totalRoles: 0,
        totalPermissions: 0,
        recentUsers: []
      };
    } catch (error) {
      console.error('获取仪表盘统计数据失败:', error);
      return {
        totalUsers: 0,
        activeUsers: 0,
        onlineUsers: 0,
        totalRoles: 0,
        totalPermissions: 0,
        recentUsers: []
      };
    }
  }

  /**
   * 检查用户是否有指定权限
   * @param permission 权限编码
   */
  static hasPermission(permission: string): boolean {
    // 这里可以从localStorage或context中获取用户权限列表
    // 暂时返回true，后续可以完善权限检查逻辑
    return PermissionUtil.hasPermission(permission);
  }

  /**
   * 过滤有权限的菜单项
   * @param menus 菜单列表
   */
  static filterMenusByPermission(menus: MenuItem[]): MenuItem[] {
    return menus.filter(menu => {
      // 检查当前菜单是否有权限
      const hasMenuPermission = this.hasPermission(menu.permCode);
      
      // 如果有子菜单，递归检查
      if (menu.children && menu.children.length > 0) {
        menu.children = this.filterMenusByPermission(menu.children);
        // 如果子菜单都被过滤掉了，且当前菜单也没有权限，则隐藏当前菜单
        return hasMenuPermission || menu.children.length > 0;
      }
      
      return hasMenuPermission;
    });
  }

  /**
   * 将菜单数据转换为Ant Design Menu组件需要的格式
   * @param menus 菜单列表
   */
  static convertToAntdMenuItems(menus: MenuItem[]): AntdMenuItem[] {
    return menus.map(menu => ({
      key: menu.permCode,
      label: menu.permName,
      icon: this.getMenuIcon(menu.permCode, menu),
      children: menu.children ? this.convertToAntdMenuItems(menu.children) : undefined
    }));
  }

  /**
   * 根据权限编码获取菜单图标
   * @param permCode 权限编码
   */
  private static getMenuIcon(permCode: string, menu?: MenuItem): any {
    // 优先使用数据库中的图标（如果存在）
    if (menu && menu.icon) {
      // 尝试获取Ant Design图标
      try {
        // 根据图标名称获取组件
        const iconName = menu.icon.trim();
        const IconComponent = require('@ant-design/icons')[iconName];
        if (IconComponent) {
          return React.createElement(IconComponent);
        }
      } catch (error) {
        console.warn(`图标 ${menu.icon} 不存在，使用默认图标`, error);
      }
    }
    
    const iconMap: { [key: string]: any } = {
      'DASHBOARD': DashboardOutlined,
      'USER': UserOutlined,
      'ROLE': TeamOutlined,
      'PERMISSION': SafetyOutlined,
      'SYSTEM': SettingOutlined,
      'OPENAPI': ApiOutlined,
      'OPENAPI_MANAGE': ApiOutlined,
      'OPENAPI_CREDENTIAL': KeyOutlined,
      'OPENAPI_RESOURCE': AppstoreOutlined,
      'OPENAPI_PERMISSION': SafetyOutlined
    };

    // 直接匹配完整的权限编码
    if (iconMap[permCode]) {
      return React.createElement(iconMap[permCode]);
    }
    
    // 部分匹配
    for (const [key, IconComponent] of Object.entries(iconMap)) {
      if (permCode.toUpperCase().includes(key)) {
        return React.createElement(IconComponent);
      }
    }

    // 默认图标
    return React.createElement(MenuOutlined);
  }
} 
package com.webapp.security.admin.controller.dashboard.vo;

import com.webapp.security.admin.controller.user.vo.UserVO;
import lombok.Data;

import java.util.List;

/**
 * 仪表盘统计数据VO
 */
@Data
public class DashboardStatVO {
    /**
     * 用户总数
     */
    private long totalUsers;

    /**
     * 活跃用户数
     */
    private long activeUsers;

    /**
     * 在线用户数
     */
    private long onlineUsers;

    /**
     * 角色总数
     */
    private long totalRoles;

    /**
     * 权限总数
     */
    private long totalPermissions;

    /**
     * 最近新增用户
     */
    private List<UserVO> recentUsers;
}
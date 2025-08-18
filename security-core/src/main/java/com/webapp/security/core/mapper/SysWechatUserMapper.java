package com.webapp.security.core.mapper;

import com.webapp.security.core.entity.SysWechatUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 微信用户关联数据访问接口
 */
@Mapper
public interface SysWechatUserMapper {

    /**
     * 根据ID查询
     */
    SysWechatUser selectById(Long id);

    /**
     * 根据OpenID查询
     */
    SysWechatUser selectByOpenid(String openid);

    /**
     * 根据UnionID查询
     */
    SysWechatUser selectByUnionid(String unionid);

    /**
     * 根据用户ID查询
     */
    SysWechatUser selectByUserId(Long userId);

    /**
     * 插入新记录
     */
    int insert(SysWechatUser wechatUser);

    /**
     * 更新记录
     */
    int update(SysWechatUser wechatUser);

    /**
     * 删除记录
     */
    int deleteById(Long id);

    /**
     * 根据OpenID删除记录
     */
    int deleteByOpenid(String openid);

    /**
     * 根据用户ID删除记录
     */
    int deleteByUserId(Long userId);
}
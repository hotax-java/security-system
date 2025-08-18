package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysAlipayUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 支付宝用户Mapper接口
 */
@Mapper
public interface SysAlipayUserMapper extends BaseMapper<SysAlipayUser> {
    /**
     * 根据支付宝用户ID查询
     */
    @Select("SELECT * FROM sys_alipay_user WHERE alipay_user_id = #{alipayUserId} LIMIT 1")
    SysAlipayUser selectByAlipayUserId(@Param("alipayUserId") String alipayUserId);

    /**
     * 根据系统用户ID查询
     */
    @Select("SELECT * FROM sys_alipay_user WHERE user_id = #{userId} LIMIT 1")
    SysAlipayUser selectByUserId(@Param("userId") Long userId);

    /**
     * 绑定系统用户
     */
    @Update("UPDATE sys_alipay_user SET user_id = #{userId}, update_time = NOW() WHERE id = #{id}")
    int bindUser(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 更新令牌
     */
    @Update("UPDATE sys_alipay_user SET access_token = #{accessToken}, refresh_token = #{refreshToken}, update_time = NOW() WHERE id = #{id}")
    int updateToken(@Param("id") Long id, @Param("accessToken") String accessToken,
            @Param("refreshToken") String refreshToken);
}
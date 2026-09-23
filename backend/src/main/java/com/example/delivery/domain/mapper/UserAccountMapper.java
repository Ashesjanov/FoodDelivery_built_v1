package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.UserAccount;

/**
 * user_account 表的 MyBatis-Plus mapper，供注册、登录、JWT 复核和账号管理使用。
 * 密码只以哈希保存；认证过滤器每次校验令牌时会通过该 mapper 读取账号状态。
 */
public interface UserAccountMapper extends BaseMapper<UserAccount> {
}

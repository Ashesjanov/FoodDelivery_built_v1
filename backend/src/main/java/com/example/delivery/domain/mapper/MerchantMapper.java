package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Merchant;

/**
 * merchant 表的 MyBatis-Plus mapper，供商户入驻、营业管理与门店查询使用。
 * 营业状态、费用和位置字段的校验及权限检查位于商户服务层。
 */
public interface MerchantMapper extends BaseMapper<Merchant> {
}

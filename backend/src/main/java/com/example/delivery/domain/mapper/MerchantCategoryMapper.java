package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.MerchantCategory;

/**
 * merchant_category 表的 MyBatis-Plus mapper，供平台维护商户分类字典。
 * 前台分类查询可与 MerchantCategoryRelMapper 组合，映射实现由 MapperScan 提供。
 */
public interface MerchantCategoryMapper extends BaseMapper<MerchantCategory> {
}

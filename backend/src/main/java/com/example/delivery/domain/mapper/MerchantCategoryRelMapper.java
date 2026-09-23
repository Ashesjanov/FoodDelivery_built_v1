package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.MerchantCategoryRel;

/**
 * merchant_category_rel 关联表的 MyBatis-Plus mapper，维护商户与分类的多对多关系。
 * 调整关联时由 service 保证同一商户的排序值和分类有效性。
 */
public interface MerchantCategoryRelMapper extends BaseMapper<MerchantCategoryRel> {
}

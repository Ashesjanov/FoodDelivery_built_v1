package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.Review;

/**
 * review 表的 MyBatis-Plus mapper，供订单评价、商户回复和前台展示查询使用。
 * 评价可见性由 ReviewStatus 控制，写入前的一单一评校验位于评价服务层。
 */
public interface ReviewMapper extends BaseMapper<Review> {
}

package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户与分类的关联实体，映射 merchant_category_rel 表，支持一家商户属于多个分类。
 * 由商户服务通过 MerchantCategoryRelMapper 维护，关系调整需同时保证分类有效性和排序。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("merchant_category_rel")
public class MerchantCategoryRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long categoryId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}

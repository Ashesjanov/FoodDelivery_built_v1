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
 * 商户分类字典实体，映射 merchant_category 表，保存前台分类名称、图标和排序。
 * 由商户服务通过 MerchantCategoryMapper 维护；停用分类不应继续参与前台筛选。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("merchant_category")
public class MerchantCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

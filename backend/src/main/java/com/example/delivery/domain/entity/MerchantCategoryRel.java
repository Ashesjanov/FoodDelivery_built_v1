package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

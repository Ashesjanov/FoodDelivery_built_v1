package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.ReviewStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单评价实体，映射 review 表，保存用户评分、内容、图片和商户回复。
 * 由评价服务通过 ReviewMapper 维护；一单一次评价和展示状态由服务层共同约束。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private Integer rating;
    private String content;
    private String imageUrl;
    private String replyContent;
    private ReviewStatus status;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

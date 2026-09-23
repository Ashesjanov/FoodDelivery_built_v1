package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 评价可见、隐藏和软删除状态，供用户评价列表与商户回复筛选使用。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 review.status。
 */
public enum ReviewStatus {
    VISIBLE("VISIBLE"), HIDDEN("HIDDEN"), DELETED("DELETED");

    @EnumValue
    private final String code;

    ReviewStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

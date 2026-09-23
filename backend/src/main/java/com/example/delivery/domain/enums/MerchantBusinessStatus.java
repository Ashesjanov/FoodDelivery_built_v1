package com.example.delivery.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 商户备餐、营业、暂停接单和关闭状态，控制下单资格及前台展示。
 * code 字段通过 MyBatis-Plus 的 @EnumValue 映射 merchant.business_status。
 */
public enum MerchantBusinessStatus {
    PREPARING("PREPARING"), OPEN("OPEN"), PAUSED("PAUSED"), CLOSED("CLOSED");

    @EnumValue
    private final String code;

    MerchantBusinessStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

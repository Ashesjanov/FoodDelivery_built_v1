package com.example.delivery.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.delivery.domain.entity.PaymentRecord;

/**
 * payment_record 表的 MyBatis-Plus mapper，供支付、回调和退款服务记录资金流水。
 * 支付状态必须幂等更新，外部交易号由支付服务负责唯一性检查。
 */
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}

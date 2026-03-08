package cn.edu.distcourse.flashsale.model;

import lombok.Data;

/**
 * 秒杀防重购记录实体，对应 t_flash_guard 表
 * 通过 (member_id, product_id) 联合唯一索引防止同一用户重复购买同一商品
 */
@Data
public class FlashGuard {

    private Long id;

    /** 购买用户ID */
    private Long memberId;

    /** 关联订单ID，外键关联 t_trade_order */
    private Long orderId;

    /** 购买商品ID */
    private Long productId;
}

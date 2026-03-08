package cn.edu.distcourse.flashsale.model;

import lombok.Data;

/**
 * Idempotency guard for flash-sale orders.
 * A unique index on (member_id, product_id) prevents duplicate purchases at the DB level.
 * Maps to {@code t_flash_guard}.
 */
@Data
public class FlashGuard {

    private Long id;

    private Long memberId;

    /** FK to t_trade_order */
    private Long orderId;

    private Long productId;
}

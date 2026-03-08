package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Completed trade order — maps to {@code t_trade_order}. */
@Data
public class TradeOrder {

    private Long id;

    private Long memberId;

    private Long sellerId;

    private Long productId;

    /** Snapshot of product name at the time of purchase */
    private String productName;

    private BigDecimal amount;

    /** 0-pending, 1-paid, 2-shipped, 3-refunded */
    private Integer status;

    private LocalDateTime createdAt;
}

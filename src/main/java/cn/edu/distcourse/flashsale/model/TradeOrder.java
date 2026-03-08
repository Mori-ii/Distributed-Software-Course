package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单实体，对应 t_trade_order 表
 */
@Data
public class TradeOrder {

    private Long id;

    /** 下单用户ID */
    private Long memberId;

    /** 商家ID */
    private Long sellerId;

    /** 商品ID */
    private Long productId;

    /** 商品名称快照（下单时记录，防止后续改名影响） */
    private String productName;

    /** 实际支付金额 */
    private BigDecimal amount;

    /** 订单状态：0-待付款, 1-已付款, 2-已发货, 3-已退款 */
    private Integer status;

    /** 下单时间 */
    private LocalDateTime createdAt;
}

package cn.edu.distcourse.flashsale.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图对象
 * 合并商品基本信息与秒杀活动配置信息
 */
@Data
public class FlashProductVO {

    /** 商品ID */
    private Long productId;
    /** 商品名称 */
    private String productName;
    /** 商品图片地址 */
    private String imageUrl;
    /** 原始零售价 */
    private BigDecimal originalPrice;

    /** 秒杀活动ID */
    private Long flashId;
    /** 秒杀特价 */
    private BigDecimal flashPrice;
    /** 秒杀剩余库存 */
    private Integer remaining;
    /** 秒杀开始时间 */
    private LocalDateTime beginAt;
    /** 秒杀结束时间 */
    private LocalDateTime finishAt;
}

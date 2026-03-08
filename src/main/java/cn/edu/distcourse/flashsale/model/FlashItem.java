package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动配置实体，对应 t_flash_item 表
 * 秒杀库存与商品主库存分离（冷热数据分离设计）
 */
@Data
public class FlashItem {

    private Long id;

    /** 关联商品ID，外键关联 t_product */
    private Long productId;

    /** 秒杀特价 */
    private BigDecimal flashPrice;

    /** 秒杀剩余库存 */
    private Integer remaining;

    /** 秒杀开始时间 */
    private LocalDateTime beginAt;

    /** 秒杀结束时间 */
    private LocalDateTime finishAt;

    /** 乐观锁版本号，每次扣减库存时+1 */
    private Integer version;
}

package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Flash-sale item configuration — maps to {@code t_flash_item}. */
@Data
public class FlashItem {

    private Long id;

    /** FK to t_product */
    private Long productId;

    /** Discounted flash price */
    private BigDecimal flashPrice;

    private Integer remaining;

    private LocalDateTime beginAt;

    private LocalDateTime finishAt;

    /** Optimistic-lock version */
    private Integer version;
}

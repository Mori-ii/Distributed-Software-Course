package cn.edu.distcourse.flashsale.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** View object that merges product info with flash-sale config. */
@Data
public class FlashProductVO {

    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal originalPrice;

    private Long flashId;
    private BigDecimal flashPrice;
    private Integer remaining;
    private LocalDateTime beginAt;
    private LocalDateTime finishAt;
}

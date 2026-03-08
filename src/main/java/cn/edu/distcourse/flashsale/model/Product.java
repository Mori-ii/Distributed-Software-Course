package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;

/** Regular product catalog entry — maps to {@code t_product}. */
@Data
public class Product {

    private Long id;

    /** FK to t_seller */
    private Long sellerId;

    private String productName;

    private String imageUrl;

    /** Original retail price */
    private BigDecimal price;

    private Integer stock;
}

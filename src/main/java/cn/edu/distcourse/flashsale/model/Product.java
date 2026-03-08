package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品实体，对应 t_product 表
 */
@Data
public class Product {

    private Long id;

    /** 所属商家ID，外键关联 t_seller */
    private Long sellerId;

    /** 商品名称 */
    private String productName;

    /** 商品图片地址 */
    private String imageUrl;

    /** 原始零售价 */
    private BigDecimal price;

    /** 普通库存数量 */
    private Integer stock;
}

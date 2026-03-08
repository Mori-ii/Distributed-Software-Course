package cn.edu.distcourse.flashsale.model;

import lombok.Data;

/** Seller / shop information — maps to {@code t_seller}. */
@Data
public class Seller {

    private Long id;

    private String shopName;

    /** 0 = inactive, 1 = active */
    private Integer status;
}

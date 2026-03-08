package cn.edu.distcourse.flashsale.model;

import lombok.Data;

/**
 * 商家实体，对应 t_seller 表
 */
@Data
public class Seller {

    private Long id;

    /** 店铺名称 */
    private String shopName;

    /** 状态：0-停用, 1-启用 */
    private Integer status;
}

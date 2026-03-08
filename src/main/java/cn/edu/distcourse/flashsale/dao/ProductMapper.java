package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品数据访问接口
 */
@Mapper
public interface ProductMapper {

    /** 根据ID查询商品 */
    @Select("SELECT * FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    /** 查询全部商品 */
    @Select("SELECT * FROM t_product")
    List<Product> findAll();

    /** 根据商家ID查询商品列表 */
    @Select("SELECT * FROM t_product WHERE seller_id = #{sellerId}")
    List<Product> findBySellerId(Long sellerId);

    /** 新增商品 */
    @Insert("INSERT INTO t_product(seller_id, product_name, image_url, price, stock) " +
            "VALUES(#{sellerId}, #{productName}, #{imageUrl}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Product product);

    /** 更新商品信息 */
    @Update("UPDATE t_product SET product_name = #{productName}, image_url = #{imageUrl}, " +
            "price = #{price}, stock = #{stock} WHERE id = #{id}")
    int modify(Product product);
}

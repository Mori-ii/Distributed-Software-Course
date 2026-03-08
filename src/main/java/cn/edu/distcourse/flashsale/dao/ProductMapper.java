package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    @Select("SELECT * FROM t_product")
    List<Product> findAll();

    @Select("SELECT * FROM t_product WHERE seller_id = #{sellerId}")
    List<Product> findBySellerId(Long sellerId);

    @Insert("INSERT INTO t_product(seller_id, product_name, image_url, price, stock) " +
            "VALUES(#{sellerId}, #{productName}, #{imageUrl}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Product product);

    @Update("UPDATE t_product SET product_name = #{productName}, image_url = #{imageUrl}, " +
            "price = #{price}, stock = #{stock} WHERE id = #{id}")
    int modify(Product product);
}

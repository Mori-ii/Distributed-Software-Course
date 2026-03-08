package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Seller;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SellerMapper {

    @Select("SELECT * FROM t_seller WHERE id = #{id}")
    Seller findById(Long id);

    @Select("SELECT * FROM t_seller WHERE status = 1")
    List<Seller> findAllActive();

    @Insert("INSERT INTO t_seller(shop_name, status) VALUES(#{shopName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Seller seller);

    @Update("UPDATE t_seller SET status = #{status} WHERE id = #{id}")
    int changeStatus(@Param("id") Long id, @Param("status") Integer status);
}

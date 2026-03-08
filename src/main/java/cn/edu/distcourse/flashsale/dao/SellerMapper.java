package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Seller;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商家数据访问接口
 */
@Mapper
public interface SellerMapper {

    /** 根据ID查询商家 */
    @Select("SELECT * FROM t_seller WHERE id = #{id}")
    Seller findById(Long id);

    /** 查询所有启用状态的商家 */
    @Select("SELECT * FROM t_seller WHERE status = 1")
    List<Seller> findAllActive();

    /** 新增商家 */
    @Insert("INSERT INTO t_seller(shop_name, status) VALUES(#{shopName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Seller seller);

    /** 修改商家状态 */
    @Update("UPDATE t_seller SET status = #{status} WHERE id = #{id}")
    int changeStatus(@Param("id") Long id, @Param("status") Integer status);
}

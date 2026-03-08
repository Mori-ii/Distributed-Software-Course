package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.FlashGuard;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FlashGuardMapper {

    /**
     * Check whether a member has already purchased the given product.
     *
     * @return non-null if already purchased
     */
    @Select("SELECT * FROM t_flash_guard WHERE member_id = #{memberId} AND product_id = #{productId}")
    FlashGuard findByMemberAndProduct(@Param("memberId") Long memberId,
                                      @Param("productId") Long productId);

    @Insert("INSERT INTO t_flash_guard(member_id, order_id, product_id) " +
            "VALUES(#{memberId}, #{orderId}, #{productId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(FlashGuard guard);
}

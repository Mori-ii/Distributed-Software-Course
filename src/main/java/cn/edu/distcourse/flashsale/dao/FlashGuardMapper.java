package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.FlashGuard;
import org.apache.ibatis.annotations.*;

/**
 * 秒杀防重购数据访问接口
 */
@Mapper
public interface FlashGuardMapper {

    /**
     * 检查用户是否已购买过指定商品
     *
     * @return 非 null 表示已购买过
     */
    @Select("SELECT * FROM t_flash_guard WHERE member_id = #{memberId} AND product_id = #{productId}")
    FlashGuard findByMemberAndProduct(@Param("memberId") Long memberId,
                                      @Param("productId") Long productId);

    /** 新增防重购记录 */
    @Insert("INSERT INTO t_flash_guard(member_id, order_id, product_id) " +
            "VALUES(#{memberId}, #{orderId}, #{productId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(FlashGuard guard);
}

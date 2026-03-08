package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.TradeOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单数据访问接口
 */
@Mapper
public interface TradeOrderMapper {

    /** 根据ID查询订单 */
    @Select("SELECT * FROM t_trade_order WHERE id = #{id}")
    TradeOrder findById(Long id);

    /** 根据用户ID查询订单列表 */
    @Select("SELECT * FROM t_trade_order WHERE member_id = #{memberId}")
    List<TradeOrder> findByMemberId(Long memberId);

    /** 新增订单 */
    @Insert("INSERT INTO t_trade_order(member_id, seller_id, product_id, product_name, amount, status) " +
            "VALUES(#{memberId}, #{sellerId}, #{productId}, #{productName}, #{amount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(TradeOrder order);

    /** 修改订单状态 */
    @Update("UPDATE t_trade_order SET status = #{status} WHERE id = #{id}")
    int changeStatus(@Param("id") Long id, @Param("status") Integer status);
}

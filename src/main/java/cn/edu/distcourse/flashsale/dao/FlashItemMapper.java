package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.FlashItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 秒杀活动配置数据访问接口
 */
@Mapper
public interface FlashItemMapper {

    /** 根据ID查询秒杀配置 */
    @Select("SELECT * FROM t_flash_item WHERE id = #{id}")
    FlashItem findById(Long id);

    /** 根据商品ID查询秒杀配置 */
    @Select("SELECT * FROM t_flash_item WHERE product_id = #{productId}")
    FlashItem findByProductId(Long productId);

    /** 查询当前时间窗口内生效的秒杀活动 */
    @Select("SELECT * FROM t_flash_item WHERE begin_at <= NOW() AND finish_at >= NOW()")
    List<FlashItem> findActive();

    /** 新增秒杀配置 */
    @Insert("INSERT INTO t_flash_item(product_id, flash_price, remaining, begin_at, finish_at, version) " +
            "VALUES(#{productId}, #{flashPrice}, #{remaining}, #{beginAt}, #{finishAt}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(FlashItem item);

    /**
     * 乐观锁扣减库存
     * 条件：剩余库存 > 0 且版本号匹配
     * 成功时版本号自动+1
     *
     * @return 1-扣减成功, 0-版本冲突或已售罄
     */
    @Update("UPDATE t_flash_item SET remaining = remaining - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}

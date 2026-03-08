package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.FlashItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FlashItemMapper {

    @Select("SELECT * FROM t_flash_item WHERE id = #{id}")
    FlashItem findById(Long id);

    @Select("SELECT * FROM t_flash_item WHERE product_id = #{productId}")
    FlashItem findByProductId(Long productId);

    /** Return all items whose flash window covers the current moment. */
    @Select("SELECT * FROM t_flash_item WHERE begin_at <= NOW() AND finish_at >= NOW()")
    List<FlashItem> findActive();

    @Insert("INSERT INTO t_flash_item(product_id, flash_price, remaining, begin_at, finish_at, version) " +
            "VALUES(#{productId}, #{flashPrice}, #{remaining}, #{beginAt}, #{finishAt}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(FlashItem item);

    /**
     * Optimistic-lock stock deduction.
     * Conditions: remaining > 0 AND version matches.
     * On success the version is bumped by 1.
     *
     * @return 1 if deducted, 0 if conflict or sold out
     */
    @Update("UPDATE t_flash_item SET remaining = remaining - 1, version = version + 1 " +
            "WHERE id = #{id} AND remaining > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}

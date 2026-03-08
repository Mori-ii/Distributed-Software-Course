package cn.edu.distcourse.flashsale.service;

import cn.edu.distcourse.flashsale.dao.FlashItemMapper;
import cn.edu.distcourse.flashsale.dao.ProductMapper;
import cn.edu.distcourse.flashsale.model.FlashItem;
import cn.edu.distcourse.flashsale.model.Product;
import cn.edu.distcourse.flashsale.vo.FlashProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final FlashItemMapper flashItemMapper;

    /**
     * 查询当前生效的秒杀商品列表
     * 将秒杀配置与商品基本信息合并为视图对象返回
     */
    public List<FlashProductVO> listActiveFlashProducts() {
        List<FlashItem> activeItems = flashItemMapper.findActive();
        List<FlashProductVO> result = new ArrayList<>();

        for (FlashItem fi : activeItems) {
            Product p = productMapper.findById(fi.getProductId());
            if (p == null) continue;

            FlashProductVO vo = new FlashProductVO();
            vo.setProductId(p.getId());
            vo.setProductName(p.getProductName());
            vo.setImageUrl(p.getImageUrl());
            vo.setOriginalPrice(p.getPrice());
            vo.setFlashId(fi.getId());
            vo.setFlashPrice(fi.getFlashPrice());
            vo.setRemaining(fi.getRemaining());
            vo.setBeginAt(fi.getBeginAt());
            vo.setFinishAt(fi.getFinishAt());
            result.add(vo);
        }
        return result;
    }
}

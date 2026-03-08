package cn.edu.distcourse.flashsale.controller;

import cn.edu.distcourse.flashsale.common.ApiResponse;
import cn.edu.distcourse.flashsale.service.ProductService;
import cn.edu.distcourse.flashsale.vo.FlashProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品服务接口
 */
@RestController
@RequestMapping("/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 获取当前生效的秒杀商品列表 */
    @GetMapping("/flash/active")
    public ApiResponse<List<FlashProductVO>> activeFlashProducts() {
        return ApiResponse.ok(productService.listActiveFlashProducts());
    }
}

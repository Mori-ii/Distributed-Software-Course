package cn.edu.distcourse.flashsale.controller;

import cn.edu.distcourse.flashsale.common.ApiResponse;
import cn.edu.distcourse.flashsale.dao.TradeOrderMapper;
import cn.edu.distcourse.flashsale.model.TradeOrder;
import cn.edu.distcourse.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀抢购服务接口
 */
@RestController
@RequestMapping("/v1/flash")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final TradeOrderMapper tradeOrderMapper;

    /** 执行秒杀抢购 */
    @PostMapping("/snap")
    public ApiResponse<TradeOrder> snap(@RequestParam Long memberId,
                                        @RequestParam Long flashId) {
        try {
            TradeOrder order = flashSaleService.snap(memberId, flashId);
            return ApiResponse.ok(order);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 查询用户的历史订单 */
    @GetMapping("/my-orders")
    public ApiResponse<List<TradeOrder>> myOrders(@RequestParam Long memberId) {
        return ApiResponse.ok(tradeOrderMapper.findByMemberId(memberId));
    }
}

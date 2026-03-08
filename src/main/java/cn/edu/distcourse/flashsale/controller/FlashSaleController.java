package cn.edu.distcourse.flashsale.controller;

import cn.edu.distcourse.flashsale.common.ApiResponse;
import cn.edu.distcourse.flashsale.dao.TradeOrderMapper;
import cn.edu.distcourse.flashsale.model.TradeOrder;
import cn.edu.distcourse.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/flash")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final TradeOrderMapper tradeOrderMapper;

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

    @GetMapping("/my-orders")
    public ApiResponse<List<TradeOrder>> myOrders(@RequestParam Long memberId) {
        return ApiResponse.ok(tradeOrderMapper.findByMemberId(memberId));
    }
}

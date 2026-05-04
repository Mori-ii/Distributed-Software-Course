package com.seckill.controller;


import com.seckill.dto.Result;
import com.seckill.service.IVoucherOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀订单控制器
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 按订单ID查询订单（支持幂等性校验）
     */
    @GetMapping("/{orderId}")
    public Result queryOrderById(@PathVariable Long orderId) {
        return voucherOrderService.queryOrderById(orderId);
    }

    /**
     * 按用户ID查询该用户所有秒杀订单
     */
    @GetMapping("/user/{userId}")
    public Result queryOrdersByUserId(@PathVariable Long userId) {
        return voucherOrderService.queryOrdersByUserId(userId);
    }
}

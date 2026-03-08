package cn.edu.distcourse.flashsale.controller;

import cn.edu.distcourse.flashsale.common.ApiResponse;
import cn.edu.distcourse.flashsale.model.Member;
import cn.edu.distcourse.flashsale.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务接口（登录 / 注册）
 */
@RestController
@RequestMapping("/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** 用户登录，支持 ID 或昵称 */
    @PostMapping("/sign-in")
    public ApiResponse<Member> signIn(@RequestParam String account,
                                      @RequestParam String password) {
        try {
            Member member = accountService.signIn(account, password);
            // 脱敏处理，不返回密码和盐值
            member.setPwd(null);
            member.setSalt(null);
            return ApiResponse.ok(member);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 用户注册 */
    @PostMapping("/sign-up")
    public ApiResponse<Member> signUp(@RequestParam String alias,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return ApiResponse.fail("两次输入的密码不一致");
        }
        try {
            Member member = accountService.signUp(alias, password);
            return ApiResponse.ok(member);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}

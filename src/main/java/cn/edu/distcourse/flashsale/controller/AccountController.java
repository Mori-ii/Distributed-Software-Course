package cn.edu.distcourse.flashsale.controller;

import cn.edu.distcourse.flashsale.common.ApiResponse;
import cn.edu.distcourse.flashsale.model.Member;
import cn.edu.distcourse.flashsale.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/sign-in")
    public ApiResponse<Member> signIn(@RequestParam String account,
                                      @RequestParam String password) {
        try {
            Member member = accountService.signIn(account, password);
            member.setPwd(null);
            member.setSalt(null);
            return ApiResponse.ok(member);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/sign-up")
    public ApiResponse<Member> signUp(@RequestParam String alias,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return ApiResponse.fail("Passwords do not match");
        }
        try {
            Member member = accountService.signUp(alias, password);
            return ApiResponse.ok(member);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}

package com.seckill.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.seckill.dto.UserDTO;
import com.seckill.entity.User;
import io.netty.util.internal.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LoginInterceptor implements HandlerInterceptor {

//    private StringRedisTemplate stringRedisTemplate;
//    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
//        this.stringRedisTemplate = stringRedisTemplate;
//    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//判断是否需要去拦截（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
//            无用户，需要 拦截
            response.setStatus(401);
//            拦截
            return false;
        }
//        由用户，放行
        return true;
    }

}

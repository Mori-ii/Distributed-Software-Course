package com.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.dto.LoginFormDTO;
import com.seckill.dto.Result;
import com.seckill.dto.UserDTO;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.IUserService;
import com.seckill.utils.RegexUtils;
import com.seckill.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.seckill.utils.RedisConstants.*;
import static com.seckill.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
//        1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //        2.如果不符合，返回错误
            return Result.fail("手机号格式错误！");
        }
//        3.符合，生成验证码
        String code =RandomUtil.randomNumbers(6);

//        4. 保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

//      TODO:  5.发送验证码,实现复杂，跳过，先写个假的
        log.debug("发送验证码成功：{} ",code);
//        6.返回结果
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误！");
        }
//        2.从redis获取验证码并验证
        Object CacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if(CacheCode == null || !CacheCode.equals(code)){
            //        3.如果不一致，报错
            return Result.fail("验证码错误！");
        }
//        4.如果一致，根据手机号查询用户  select * from user where phone = ?
        User user =query().eq("phone",phone).one();
//        5.判断用户是否存在
        if( user == null){
            //        6.如果用户不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }

//        7.最终保存用户信息到redis中
//        7.1 随机生成token，作为登录令牌（用户的唯一标识）
// ❌ 错误写法
// UUID token = UUID.randomUUID().toString(true);

//      ✅ 正确写法：把类型改成 String
        String token = UUID.randomUUID().toString(true);
//        7.2将用户保存到redis中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//        7.3 存储
        String tokenKey = LOGIN_USER_KEY+token;
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((filedName,fieldValue)->fieldValue.toString()));

        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//        7.4 设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
//        创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
//        保存用户
        save(user);
        return user;
    }

    @Override
    public Result sign() {
//        1.获取当前登录的用户
        Long userId = UserHolder.getUser().getId();
//        2.获取日期
        LocalDateTime now = LocalDateTime.now();
//          3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
//        4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
//        5.写入redis setbit key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1.获取当前登录的用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        //  3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月到今天为止的签到记录，返回一个十进制数字
        List<Long> result =stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null|| num == 0){
            return Result.ok(0);
        }
//        6.循环遍历，判断这个日期是否在记录中存在
        int count = 0;
        while(true){
            //        7.让这个数字与数字1做与运算，得到数字中1对应的位置
            //        8.判断这个bit位是否为0
            if((num & 1) == 0){
                //        9.如果为0，说明未签到，结束
                break;
            }else {
                //        10.如果为1，说明已签到，计数器+1
                count++;
            }
            //        11.把数字右移一位，相当于把数字除以2
            num >>>= 1;

        }
        return Result.ok(count);

    }
}

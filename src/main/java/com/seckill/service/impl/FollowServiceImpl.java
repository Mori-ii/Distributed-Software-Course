package com.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.seckill.dto.Result;
import com.seckill.dto.UserDTO;
import com.seckill.entity.Follow;
import com.seckill.mapper.FollowMapper;
import com.seckill.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.service.IUserService;
import com.seckill.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
//获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follow:follows:" + userId;

        //        判断是否关注
        if(isFollow) {
//        关注，插入数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if(isSuccess) {
//                把关注的用户id保存到Redis的set集合  follow:follows:userId

                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
//        取关
            boolean isSuccess = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
//        取关，删除数据
        return Result.ok();
    }
    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
//        1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follow:follows:" + userId;
//        2.求交集
        String key2 = "follow:follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect == null|| intersect.isEmpty()){
//            无交集，返回空集合
            return Result.ok(new ArrayList<>());
        }
//        3.解析出id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
//        4.查询 用户
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}

package com.seckill.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.Result;
import com.seckill.dto.ScrollResult;
import com.seckill.dto.UserDTO;
import com.seckill.entity.Blog;
import com.seckill.entity.Follow;
import com.seckill.entity.User;
import com.seckill.mapper.BlogMapper;
import com.seckill.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.service.IFollowService;
import com.seckill.service.IUserService;
import com.seckill.utils.SystemConstants;
import com.seckill.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.seckill.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.seckill.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */


@Service
@Transactional
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->  {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }
    @Override
    public Result queryBlogById(Long id) {
//        1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
//        2.查询blog有关的 用户
        queryBlogUser(blog);
//        3.查询Blog是否被点赞了
        isBlogLiked(blog);
        return Result.ok(blog);
    }
    private void isBlogLiked(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        // 判断该用户是否已经点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }


    private void queryBlogUser(Blog blog) {
        try {
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        } catch (Exception e) {
            // 记录错误日志，但不中断流程
            log.warn("查询用户信息失败，userId: {}", blog.getUserId(), e);
        }
    }

    @Override
    public Result likeBlog(Long id) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("用户未登录");
        }
        Long userId = user.getId();

        // 判断该用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        if(score == null) {
            // 如果未点赞，允许点赞
            boolean isSuccess = update()
                    .set("liked", getLikedCount(id) + 1) // 获取当前点赞数并+1
                    .eq("id", id)
                    .update();
            // 数据库点赞数+1
            if(isSuccess) {
                // 保存用户到redis的set集合  blog:liked:blogId，zadd key value score
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 如果已点赞，取消点赞
            boolean isSuccess = update()
                    .set("liked", Math.max(0, getLikedCount(id) - 1)) // 确保点赞数不为负数
                    .eq("id", id)
                    .update();
            // 把用户从redis中移除
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
        }
        return Result.ok();
    }
    // 辅助方法：获取当前点赞数
    private int getLikedCount(Long blogId) {
        Blog blog = getById(blogId);
        return blog != null ? blog.getLiked() : 0;
    }

    @Override
    public Result queryBlogLikes(Long id) {
//        1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
        if(top5 == null || top5.isEmpty()) {
            return Result.ok();
        }
//        2.解析出用户id
            List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr =StrUtil.join(",", ids);

//        3.根据用户id查询用户
        List<UserDTO> userDTOs = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id,"+idStr +")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
//        4.返回
        return Result.ok(userDTOs);
    }


    @Override
    public Result saveBlog(Blog blog) {
//        获取当前用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
//        保存博客
        boolean isSuccess = save(blog);
        if(!isSuccess) {
            return Result.fail("发布笔记失败！");
        }
//        查询笔记作者的粉丝
        List<Follow> follows = followService.query().eq("follow_id", user.getId()).list();

//        推送笔记id给粉丝
        for(Follow follow : follows){
            Long userId = follow.getUserId() ;
//            推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
//        返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset){
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
            if(typedTuples == null || typedTuples.isEmpty()) {
                return Result.ok();
            }
            List<Long> ids = new ArrayList<>(typedTuples.size());

            long minTime = 0;
            int os = 1;

            for(ZSetOperations.TypedTuple<String> tuple : typedTuples){
                ids.add(Long.valueOf(tuple.getValue()));
                long time = tuple.getScore().longValue();
                if(time == minTime){
                    os++;
                }else{
                    minTime = time;
                    os = 1;
                }
            }
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query()
                .in("id", ids).last("ORDER BY FIELD(id,"+ idStr +")").list();

        for (Blog blog : blogs) {
            queryBlogUser( blog);
            isBlogLiked(blog);
        }

        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return Result.ok(r);
    }
}

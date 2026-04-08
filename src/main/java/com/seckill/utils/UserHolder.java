package com.seckill.utils;

import com.seckill.dto.UserDTO;
import com.seckill.entity.User;

public class UserHolder {
    // 1. 容器里只存 UserDTO（这是为了安全，不存密码等敏感信息）
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    // 2. 存的方法：接收 UserDTO
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    // 3. 取的方法：返回 UserDTO（不要强制转成 User，会报错的）
    public static UserDTO getUser(){
        return tl.get();
    }

    // 4. 清理方法
    public static void removeUser(){
        tl.remove();
    }
}

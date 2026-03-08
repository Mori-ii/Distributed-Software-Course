package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 t_member 表
 */
@Data
public class Member {

    /** 用户ID，系统随机分配 */
    private Long id;

    /** 用户昵称 */
    private String alias;

    /** 加密后的密码：MD5(盐值 + 明文密码) */
    private String pwd;

    /** 随机盐值 */
    private String salt;

    /** 注册时间 */
    private LocalDateTime joinedAt;
}

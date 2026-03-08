package cn.edu.distcourse.flashsale.model;

import lombok.Data;
import java.time.LocalDateTime;

/** Registered member — maps to {@code t_member}. */
@Data
public class Member {

    private Long id;

    private String alias;

    /** MD5(salt + raw password) */
    private String pwd;

    private String salt;

    private LocalDateTime joinedAt;
}

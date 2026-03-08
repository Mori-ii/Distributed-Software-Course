package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Member;
import org.apache.ibatis.annotations.*;

/**
 * 用户数据访问接口
 */
@Mapper
public interface MemberMapper {

    /** 根据ID查询用户 */
    @Select("SELECT * FROM t_member WHERE id = #{id}")
    Member findById(Long id);

    /** 根据昵称查询用户 */
    @Select("SELECT * FROM t_member WHERE alias = #{alias}")
    Member findByAlias(String alias);

    /** 新增用户（自增ID） */
    @Insert("INSERT INTO t_member(alias, pwd, salt) VALUES(#{alias}, #{pwd}, #{salt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Member member);

    /** 新增用户（指定ID） */
    @Insert("INSERT INTO t_member(id, alias, pwd, salt) VALUES(#{id}, #{alias}, #{pwd}, #{salt})")
    int saveWithId(Member member);
}

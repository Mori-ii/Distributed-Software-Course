package cn.edu.distcourse.flashsale.dao;

import cn.edu.distcourse.flashsale.model.Member;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MemberMapper {

    @Select("SELECT * FROM t_member WHERE id = #{id}")
    Member findById(Long id);

    @Select("SELECT * FROM t_member WHERE alias = #{alias}")
    Member findByAlias(String alias);

    @Insert("INSERT INTO t_member(alias, pwd, salt) VALUES(#{alias}, #{pwd}, #{salt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Member member);

    @Insert("INSERT INTO t_member(id, alias, pwd, salt) VALUES(#{id}, #{alias}, #{pwd}, #{salt})")
    int saveWithId(Member member);
}

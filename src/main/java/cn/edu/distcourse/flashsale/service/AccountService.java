package cn.edu.distcourse.flashsale.service;

import cn.edu.distcourse.flashsale.dao.MemberMapper;
import cn.edu.distcourse.flashsale.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 用户账户业务逻辑
 * 负责登录验证、注册、密码加密等
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final MemberMapper memberMapper;
    private final SecureRandom rng = new SecureRandom();

    /**
     * 用户登录
     * 支持使用数字ID或昵称登录
     *
     * @param account     用户ID或昵称
     * @param rawPassword 明文密码
     * @return 验证通过的用户信息
     */
    public Member signIn(String account, String rawPassword) {
        Member m = null;
        // 如果输入的是纯数字，先按ID查询
        if (account.matches("\\d+")) {
            m = memberMapper.findById(Long.parseLong(account));
        }
        // ID 查不到则按昵称查询
        if (m == null) {
            m = memberMapper.findByAlias(account);
        }
        if (m == null) {
            throw new RuntimeException("账号不存在");
        }
        // 密码验证：MD5(盐值 + 明文密码)
        if (!hashPassword(m.getSalt(), rawPassword).equals(m.getPwd())) {
            throw new RuntimeException("密码错误");
        }
        return m;
    }

    /**
     * 用户注册
     * 自动分配一个随机数字ID（范围 1000 ~ 9999999）
     *
     * @param alias       用户昵称
     * @param rawPassword 明文密码
     * @return 注册成功的用户信息（已脱敏）
     */
    public Member signUp(String alias, String rawPassword) {
        if (alias == null || alias.isBlank()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (alias.matches("\\d+")) {
            throw new RuntimeException("昵称不能是纯数字");
        }
        if (alias.length() > 50) {
            throw new RuntimeException("昵称过长，最多50个字符");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }
        if (memberMapper.findByAlias(alias) != null) {
            throw new RuntimeException("该昵称已被使用");
        }

        // 生成随机盐值（6位十六进制）
        String salt = HexFormat.of().formatHex(randomBytes(3));
        String hashed = hashPassword(salt, rawPassword);
        Long id = allocateId();

        Member member = new Member();
        member.setId(id);
        member.setAlias(alias);
        member.setPwd(hashed);
        member.setSalt(salt);
        memberMapper.saveWithId(member);

        // 脱敏处理，不返回密码和盐值
        member.setPwd(null);
        member.setSalt(null);
        return member;
    }

    // === 私有辅助方法 ===

    /** 分配一个唯一的随机数字ID */
    private Long allocateId() {
        for (int attempt = 0; attempt < 100; attempt++) {
            long candidate = 1000 + rng.nextLong(9_999_000);
            if (memberMapper.findById(candidate) == null) {
                return candidate;
            }
        }
        throw new RuntimeException("分配用户ID失败，请重试");
    }

    /** 生成指定长度的随机字节 */
    private byte[] randomBytes(int n) {
        byte[] buf = new byte[n];
        rng.nextBytes(buf);
        return buf;
    }

    /** 密码哈希：MD5(盐值 + 明文密码) */
    private String hashPassword(String salt, String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest((salt + raw).getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

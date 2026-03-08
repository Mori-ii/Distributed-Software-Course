package cn.edu.distcourse.flashsale.service;

import cn.edu.distcourse.flashsale.dao.MemberMapper;
import cn.edu.distcourse.flashsale.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final MemberMapper memberMapper;
    private final SecureRandom rng = new SecureRandom();

    /**
     * Sign in with either numeric member-ID or alias.
     */
    public Member signIn(String account, String rawPassword) {
        Member m = null;
        if (account.matches("\\d+")) {
            m = memberMapper.findById(Long.parseLong(account));
        }
        if (m == null) {
            m = memberMapper.findByAlias(account);
        }
        if (m == null) {
            throw new RuntimeException("Account not found");
        }
        if (!hashPassword(m.getSalt(), rawPassword).equals(m.getPwd())) {
            throw new RuntimeException("Wrong password");
        }
        return m;
    }

    /**
     * Register a new member. A random numeric ID in [1000, 9999999] is assigned.
     */
    public Member signUp(String alias, String rawPassword) {
        if (alias == null || alias.isBlank()) {
            throw new RuntimeException("Alias cannot be empty");
        }
        if (alias.matches("\\d+")) {
            throw new RuntimeException("Alias must not be purely numeric");
        }
        if (alias.length() > 50) {
            throw new RuntimeException("Alias too long (max 50 characters)");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (memberMapper.findByAlias(alias) != null) {
            throw new RuntimeException("Alias already taken");
        }

        String salt = HexFormat.of().formatHex(randomBytes(3));
        String hashed = hashPassword(salt, rawPassword);
        Long id = allocateId();

        Member member = new Member();
        member.setId(id);
        member.setAlias(alias);
        member.setPwd(hashed);
        member.setSalt(salt);
        memberMapper.saveWithId(member);

        // Do not leak credentials
        member.setPwd(null);
        member.setSalt(null);
        return member;
    }

    // --- private helpers ---

    private Long allocateId() {
        for (int attempt = 0; attempt < 100; attempt++) {
            long candidate = 1000 + rng.nextLong(9_999_000);
            if (memberMapper.findById(candidate) == null) {
                return candidate;
            }
        }
        throw new RuntimeException("Failed to allocate member ID, please retry");
    }

    private byte[] randomBytes(int n) {
        byte[] buf = new byte[n];
        rng.nextBytes(buf);
        return buf;
    }

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

package io.pubmed.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    // 使用 HS512 算法生成密钥
    private final SecretKey secretKey = Keys.hmacShaKeyFor(
            "thisIsAVerySecureAndLongSecretKeyForHS512ThatIsAtLeast64BytesLong1234567890"
                    .getBytes(StandardCharsets.UTF_8)
    );

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @param role     角色
     * @return JWT Token
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role) // 确保角色名称与 SecurityConfig 中一致
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 天过期
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 根据 Token 创建 Authentication 对象
     *
     * @param token JWT Token
     * @return Authentication 对象
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.get("username", String.class);  // 从 claim 中获取用户名
        String role = claims.get("role", String.class);

        // 创建授权列表
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token
     * @return 角色名称
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class); // 获取角色
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 记录异常信息以便调试
            System.err.println("Invalid JWT Token: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject(); // 获取 Subject（用户名）
    }
}

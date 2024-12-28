package io.pubmed.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 提取 JWT Token
            String token = getJwtFromRequest(request);

            System.out.println(token);
            // 验证 Token
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token); // 提取角色

                // 将角色转为 GrantedAuthority 格式
                List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(role);

                // 创建 Authentication 对象并设置到 SecurityContext 中
                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authenticated user: " + authentication.getName());
                authentication.getAuthorities().forEach(auth ->
                        System.out.println("Granted Authority: " + auth.getAuthority())
                );
            }
            else {
                if (jwtTokenProvider.validateToken(token)){
                    System.out.println("hhh");
                }else{
                    System.out.println("lll");
                }
                // 打印日志，确认是否解析失败
                logger.warn("JWT 验证失败或未携带 JWT");
            }
        } catch (Exception ex) {
            logger.error("无法设置用户认证信息", ex);
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取 JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization"); // 获取请求头中的 Authorization
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 提取 JWT Token
        }
        return null;
    }
}

package io.pubmed.config;

import io.pubmed.security.JwtAuthenticationFilter;
import io.pubmed.security.JwtTokenProvider;
import io.pubmed.security.RestAuthenticationEntryPoint;
import io.pubmed.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder; // 建议更换为 BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private UserService userService;

    @Autowired
    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 使用 UserService 加载用户
        auth.userDetailsService(new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // 从 UserService 中加载用户
                var user = userService.findByUsername(username);
                if (user == null) {
                    throw new UsernameNotFoundException("User not found with username: " + username);
                }

                // 将 User 转换为 UserDetails
                return org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(user.getRole())
                        .build();
            }
        }).passwordEncoder(passwordEncoder());
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        // 强烈建议使用 BCrypt 或其他更安全的密码编码器
        return NoOpPasswordEncoder.getInstance(); // 仅用于开发测试，生产环境请更换
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .cors()
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/api/users/register", "/api/users/login").permitAll()
                // 期刊管理员，可以访问期刊的所有接口
                .antMatchers("/api/grants/**").hasAnyAuthority("SITE_ADMIN")
                .antMatchers("/api/keywords/**").hasAnyAuthority( "SITE_ADMIN")
                .antMatchers("/api/journals/**").hasAnyAuthority("JOURNAL_ADMIN","SITE_ADMIN")
                .antMatchers("/api/authors/**").hasAnyAuthority("JOURNAL_ADMIN", "ARTICLE_ADMIN","SITE_ADMIN")
                .antMatchers("/api/articles/**").hasAnyAuthority("JOURNAL_ADMIN","ARTICLE_ADMIN", "READER", "SITE_ADMIN")
                .anyRequest().authenticated()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 无状态
    }


    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}

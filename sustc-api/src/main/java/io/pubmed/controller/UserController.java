package io.pubmed.controller;

import io.pubmed.dto.JwtResponse;
import io.pubmed.dto.User;
import io.pubmed.security.JwtTokenProvider;
import io.pubmed.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器，处理与用户相关的请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private JwtTokenProvider jwtTokenProvider; // 注入 JwtTokenProvider

    @Autowired
    private UserService userService;

    /**
     * 用户登录接口
     * @param loginRequest 包含用户名和密码的请求体
     * @return 登录成功与否
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Username or password is missing");
        }

        boolean isValid = userService.login(username, password);

        if (isValid) {
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(401).body("User not found");
            }

            // 使用 JwtTokenProvider 生成 Token
            String token = jwtTokenProvider.generateToken(username, user.getRole());

            System.out.println("Generated JWT: " + token);
            return ResponseEntity.ok(Map.of("token", token, "role", user.getRole()));
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    /**
     * 用户注册接口
     * @param user 用户对象（包含用户名、密码、角色等）
     * @return 注册后的用户对象
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            // 不使用密码加密，直接保存
            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            // 添加错误日志
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户信息
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }
}

package io.pubmed.controller;

import io.pubmed.dto.User;
import io.pubmed.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器，处理与用户相关的请求
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录接口
     * @param loginRequest 包含用户名和密码的请求体
     * @return 登录成功与否
     */
    @PostMapping("/login")
    public boolean login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 调用 service 层验证登录
        return userService.login(username, password);
    }

    /**
     * 用户注册接口
     * @param user 用户对象（包含用户名、密码、角色等）
     * @return 注册后的用户对象
     */
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户对象
     */
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }
}

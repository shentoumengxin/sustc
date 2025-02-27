package io.pubmed.service.impl;

import io.pubmed.dto.User;
import io.pubmed.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private DataSource dataSource;
    // 创建表的SQL语句
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS public.users " +
                    "( " +
                    "id SERIAL PRIMARY KEY, " +  // 用户ID，自增
                    "username VARCHAR(50) NOT NULL UNIQUE, " +  // 用户名，唯一，不能为空
                    "password VARCHAR(255) NOT NULL, " +  // 密码，不能为空
                    "role VARCHAR(20) NOT NULL CHECK (" +
                    "role IN ('Site Admin', 'Journal Admin', 'Article Admin', 'Reader')), " +  // 限制角色字段为四个值
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +  // 记录用户创建时间
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +  // 记录用户更新时间
                    "CONSTRAINT users_role_check1 CHECK (role IN ('Site Admin', 'Journal Admin', 'Article Admin', 'Reader')) " +  // 强制角色约束
                    ");";

    // 删除表的SQL语句
    private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS public.users;";

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 执行创建表的SQL
            stmt.executeUpdate(CREATE_TABLE_SQL);
            log.info("Users table created successfully.");
        } catch (SQLException e) {
            log.error("Error creating users table.", e);
            throw new RuntimeException("Error creating users table", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 执行删除表的SQL
            stmt.executeUpdate(DROP_TABLE_SQL);
            log.info("Users table dropped successfully.");
        } catch (SQLException e) {
            log.error("Error dropping users table.", e);
        }
    }

    /**
     * 用户注册
     * @param user 用户信息（包含用户名、密码、角色）
     * @return 注册后的用户对象
     */
    @Override
    public User registerUser(User user) {
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        int userCount = 0;
        // 检查用户名是否已存在
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkUserSql)) {
            stmt.setString(1, user.getUsername());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userCount = rs.getInt(1);
            }
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("READER");
            }
        } catch (SQLException e) {
            log.error("Error checking if username exists: {}", user.getUsername(), e);
            throw new RuntimeException("Error checking username availability", e);
        }
        if (userCount > 0) {
            throw new RuntimeException("Username already exists!");
        }

        // 用户插入 SQL
        String insertUserSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
            // 插入用户数据
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());  // 直接存储明文密码
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();

            // 获取插入的用户ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getLong(1));  // 设置生成的用户ID
            }

        } catch (SQLException e) {
            log.error("Error inserting user into database: {}", user.getUsername(), e);
            throw new RuntimeException("Error registering user", e);
        }

        return user;
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 找到的用户对象
     */
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        User user = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
            }

        } catch (SQLException e) {
            log.error("Error querying user by username: {}", username, e);
        }

        return user;
    }


    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功与否
     */
    @Override
    public boolean login(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return password.equals(storedPassword); // 简单密码比较
            } else {
                log.warn("Login failed: user not found - {}", username);
                return false; // 用户名不存在
            }

        } catch (SQLException e) {
            log.error("Database error during login for user: {}", username, e);
            throw new RuntimeException("Database error during login", e);
        }
    }

    /**
     * 更新用户角色
     * @param userId 用户ID
     * @param newRole 新角色
     * @return 更新后的用户对象
     */
    @Override
    public User updateUserRole(Long userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setLong(2, userId);
            stmt.executeUpdate();

            return findById(userId);  // 更新成功后返回用户信息

        } catch (SQLException e) {
            log.error("Error updating user role: {}", userId, e);
            throw new RuntimeException("Error updating user role", e);
        }
    }

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected > 0;  // 如果删除成功，返回 true

        } catch (SQLException e) {
            log.error("Error deleting user: {}", userId, e);
            return false;
        }
    }

    private User findById(Long userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        User user = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
            }

        } catch (SQLException e) {
            log.error("Error querying user by ID: {}", userId, e);
        }

        return user;
    }
}

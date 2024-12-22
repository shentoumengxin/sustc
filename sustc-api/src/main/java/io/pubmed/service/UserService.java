package io.pubmed.service;

import io.pubmed.dto.User;

public interface UserService {

    /**
     * 注册新用户
     *
     * @param user 用户信息
     * @return 注册后的用户对象
     */
    User registerUser(User user);

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 找到的用户对象
     */
    User findByUsername(String username);

    /**
     * 用户登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @return 返回验证结果
     */
    boolean login(String username, String password);

    /**
     * 更新用户的角色
     *
     * @param userId 用户ID
     * @param newRole 新的角色
     * @return 更新后的用户对象
     */
    User updateUserRole(Long userId, String newRole);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 删除操作是否成功
     */
    boolean deleteUser(Long userId);
}

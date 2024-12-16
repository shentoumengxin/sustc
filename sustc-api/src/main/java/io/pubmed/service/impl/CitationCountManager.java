package io.pubmed.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单例组件，用于管理 Article_Citation_Count 临时表的引用计数。
 */
@Component
@Slf4j
public class CitationCountManager {

    @Autowired
    private DataSource dataSource;

    // 标识临时表是否已初始化
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 确保临时表已初始化
     */
    private void initializeTempTable() {
        if (initialized.compareAndSet(false, true)) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 创建临时表
                String createTempTableSQL = "CREATE TEMPORARY TABLE IF NOT EXISTS Article_Citation_Count (" +
                        "article_id INT PRIMARY KEY REFERENCES Article(id) ON DELETE CASCADE, " +
                        "citation_count INT NOT NULL DEFAULT 0" +
                        ");";
                stmt.execute(createTempTableSQL);
                log.info("Created temporary table Article_Citation_Count.");

                // 初始化引用计数
                String initCitationCountSQL = "INSERT INTO Article_Citation_Count (article_id, citation_count) " +
                        "SELECT reference_id, COUNT(*) " +
                        "FROM article_references " +
                        "GROUP BY reference_id " +
                        "ON CONFLICT (article_id) DO UPDATE SET citation_count = EXCLUDED.citation_count;";
                stmt.execute(initCitationCountSQL);
                log.info("Initialized citation counts in temporary table Article_Citation_Count.");

            } catch (SQLException e) {
                log.error("Error initializing temporary table Article_Citation_Count.", e);
                // 根据要求，不抛出异常，而是记录日志
            }
        }
    }

    /**
     * 获取指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @return 引用计数，如果未找到则返回0
     */
    public int getCitationCount(int articleId) {
        initializeTempTable(); // 确保临时表已初始化

        String sql = "SELECT citation_count FROM Article_Citation_Count WHERE article_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("citation_count");
            }
        } catch (SQLException e) {
            log.error("Error fetching citation count for article ID: {}", articleId, e);
            // 根据要求，不抛出异常，而是记录日志
        }
        return 0;
    }

    /**
     * 增加指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @param increment 增加的引用次数
     */
    public void incrementCitationCount(int articleId, int increment) {
        initializeTempTable(); // 确保临时表已初始化

        String sql = "UPDATE Article_Citation_Count SET citation_count = citation_count + ? WHERE article_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, increment);
            stmt.setInt(2, articleId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                // 如果记录不存在，则插入新的记录
                insertCitationCount(articleId, increment);
            }
        } catch (SQLException e) {
            log.error("Error incrementing citation count for article ID: {}", articleId, e);
            // 根据要求，不抛出异常，而是记录日志
        }
    }

    /**
     * 减少指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @param decrement 减少的引用次数
     */
    public void decrementCitationCount(int articleId, int decrement) {
        initializeTempTable(); // 确保临时表已初始化

        String sql = "UPDATE Article_Citation_Count SET citation_count = citation_count - ? WHERE article_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, decrement);
            stmt.setInt(2, articleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error decrementing citation count for article ID: {}", articleId, e);
            // 根据要求，不抛出异常，而是记录日志
        }
    }

    /**
     * 插入新的引用计数记录。
     *
     * @param articleId      文章ID
     * @param citationCount  引用计数
     */
    private void insertCitationCount(int articleId, int citationCount) {
        String sql = "INSERT INTO Article_Citation_Count (article_id, citation_count) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            stmt.setInt(2, citationCount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error inserting citation count for article ID: {}", articleId, e);
            // 根据要求，不抛出异常，而是记录日志
        }
    }

    /**
     * 在程序结束时删除临时表。
     */
    @PreDestroy
    public void cleanup() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String dropTempTableSQL = "DROP TABLE IF EXISTS Article_Citation_Count;";
            stmt.execute(dropTempTableSQL);
            log.info("Dropped temporary table Article_Citation_Count on shutdown.");

        } catch (SQLException e) {
            log.error("Error dropping temporary table Article_Citation_Count on shutdown.", e);

        }
    }
}

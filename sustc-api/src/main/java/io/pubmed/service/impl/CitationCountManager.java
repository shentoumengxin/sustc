package io.pubmed.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理 Article_Citation_Count 临时表的引用计数。
 */
@Component
@Slf4j
public class CitationCountManager {

    @Autowired
    private DataSource dataSource;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private Connection connection;

    /**
     * 异步初始化临时表
     */
    @Async
    public void initializeTempTableAsync() {
        if (initialized.compareAndSet(false, true)) {
            try {
                // 获取持久连接
                connection = dataSource.getConnection();
                Statement stmt = connection.createStatement();

                // 创建临时表
                String createTempTableSQL = "CREATE  TABLE IF NOT EXISTS Article_Citation_Count (" +
                        "article_id INT  , " +
                        "citation_count INT NOT NULL DEFAULT 0" +
                        "citation_year int not null "+
                        "primary key(article_id,citation_year)"+
                        ");";
                stmt.execute(createTempTableSQL);
                log.info("创建临时表 Article_Citation_Count 完成。");

                // 初始化引用计数
                String initCitationCountSQL = "INSERT INTO Article_Citation_Count (article_id, citation_count, citation_year) " +
                        "SELECT ar.reference_id AS article_id, " +
                        "COUNT(*) AS citation_count, " +
                        "EXTRACT(YEAR FROM a.date_created) AS citation_year " +
                        "FROM article_references ar " +
                        "JOIN Article a ON ar.article_id = a.id " +
                        "GROUP BY ar.reference_id, EXTRACT(YEAR FROM a.date_created); " ;

                stmt.execute(initCitationCountSQL);
                log.info("初始化临时表中的引用计数完成。");

            } catch (SQLException e) {
                log.error("初始化临时表失败。", e);
            }
        }
    }

    /**
     * 在程序启动时异步初始化临时表
     */
    @PostConstruct
    public void init() {
        initializeTempTableAsync();
    }

    /**
     * 获取指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @return 引用计数，如果未找到则返回0
     */

    /**
     * 获取指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @return 引用计数，如果未找到则返回0
     */
    public int getCitationCount(int articleId) {
        if (!initialized.get()) {
            log.warn("临时表尚未初始化。");
            return 0;
        }
        String sql = "SELECT citation_count FROM Article_Citation_Count WHERE article_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();
            int result=0;
            while (rs.next()) {
                result+=rs.getInt("citation_count");
            }
            return result;
        } catch (SQLException e) {
            log.error("获取文章ID {} 的引用计数失败。", articleId, e);
        }
        return 0;
    }

    /**
     * 增加指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @param increment 增加的引用次数
     */
    public void incrementCitationCount(int articleId, int increment,int year) {
        if (!initialized.get()) {
            log.warn("临时表尚未初始化。");
            return;
        }
        String sql = "UPDATE Article_Citation_Count SET citation_count = citation_count + ? WHERE article_id = ? and citation_year=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, increment);
            stmt.setInt(2, articleId);
            stmt.setInt(3, year);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                insertCitationCount(articleId, increment,year);
            }
        } catch (SQLException e) {
            log.error("增加文章ID {} 的引用计数失败。", articleId, e);
        }
    }

    /**
     * 减少指定文章的引用计数。
     *
     * @param articleId 文章ID
     * @param decrement 减少的引用次数
     */
    public void decrementCitationCount(int articleId, int decrement,int year) {
        if (!initialized.get()) {
            log.warn("临时表尚未初始化。");
            return;
        }
        String sql = "UPDATE Article_Citation_Count SET citation_count = citation_count - ? WHERE article_id = ? and citation_year=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, decrement);
            stmt.setInt(2, articleId);
            stmt.setInt(3, year);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("减少文章ID {} 的引用计数失败。", articleId, e);
        }
    }

    /**
     * 插入新的引用计数记录。
     *
     * @param articleId     文章ID
     * @param citationCount 引用计数
     */
    private void insertCitationCount(int articleId, int citationCount,int year) {
        String sql = "INSERT INTO Article_Citation_Count (article_id, citation_count,citation_year) VALUES (?, ?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            stmt.setInt(2, citationCount);
            stmt.setInt(3, year);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("插入文章ID {} 的引用计数失败。", articleId, e);
        }
    }
    /**
     * 获取指定文章在特定年份的引用次数。
     *
     * @param articleId 文章ID
     * @param year      引用发生的年份
     * @return 引用次数
     */
    public int getCitationsInYear(int articleId, int year) {
        String sql = "SELECT citation_count FROM Article_Citation_Count WHERE article_id = ? and citation_year=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            stmt.setInt(2, year);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("citations");
            }
        } catch (SQLException e) {
            log.error("获取文章ID {} 在年份 {} 的引用次数失败。", articleId, year, e);
        }
        return 0;
    }

    /**
     * 在程序结束时删除临时表。
     */
    @PreDestroy
    public void cleanup() {
        if (connection != null) {
            try (Statement stmt = connection.createStatement()) {
                String dropTempTableSQL = "DROP TABLE IF EXISTS Article_Citation_Count;";
                stmt.execute(dropTempTableSQL);
                log.info("删除临时表 Article_Citation_Count 完成。");
                connection.close();
            } catch (SQLException e) {
                log.error("删除临时表失败。", e);
            }
        }
    }
}

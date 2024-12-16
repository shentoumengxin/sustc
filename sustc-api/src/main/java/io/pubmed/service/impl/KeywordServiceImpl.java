package io.pubmed.service.impl;

import io.pubmed.service.KeywordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现 KeywordService 接口，提供关键字相关的数据库查询功能。
 */
@Service
@Slf4j
public class KeywordServiceImpl implements KeywordService {

    /**
     * 通过 Spring 注入的数据库连接池。
     */
    @Autowired
    private DataSource dataSource;

    /**
     * 根据给定关键字查询过去一年的发表文章数量，并按年份降序排列。
     *
     * @param keyword 需要查询的关键字
     * @return 包含每年文章数量的整型数组，按年份降序排列
     */
    @Override
    public int[] getArticleCountByKeywordInPastYears(String keyword) {
        String sql = "SELECT EXTRACT(YEAR FROM a.date_completed) AS year, COUNT(*) AS article_count " +
                "FROM Article a " +
                "JOIN Article_Keywords ak ON a.id = ak.article_id " +
                "JOIN Keywords k ON ak.keyword_id = k.id " +
                "WHERE k.keyword = ? "  +
                "GROUP BY year " +
                "ORDER BY year DESC";

        List<Integer> articleCounts = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置查询参数
            stmt.setString(1, keyword);

            // 执行查询
            ResultSet rs = stmt.executeQuery();

            // 处理结果集
            while (rs.next()) {
                int count = rs.getInt("article_count");
                articleCounts.add(count);
            }

        } catch (SQLException e) {
            log.error("Error querying article count by keyword: {}", keyword, e);
            throw new RuntimeException("Error querying article count by keyword", e);
        }

        // 将 List 转换为 int[] 数组
        int[] result = new int[articleCounts.size()];
        for (int i = 0; i < articleCounts.size(); i++) {
            result[i] = articleCounts.get(i);
        }

        return result;
    }
}

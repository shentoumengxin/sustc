package io.pubmed.service.impl;

import io.pubmed.dto.Journal;
import io.pubmed.service.JournalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

@Service
@Slf4j
public class JournalServiceImpl implements JournalService {

    @Autowired
    private DataSource dataSource;

    /**
     * 计算指定年份的期刊影响因子（Impact Factor）。
     *
     * @param journal_title 需要查询的期刊标题
     * @param year          需要查询的年份
     * @return 指定年份的期刊影响因子
     */
    @Override
    public double getImpactFactor(String journal_title, int year) {
        // 计算分子 A：指定年份对前两年发表文章的引用总数
        String sqlA = "SELECT SUM(ac.article_count) AS total_citations " +
                "FROM Article_Citations ac " +
                "JOIN Article a ON ac.article_id = a.id " +
                "JOIN Article_Journal aj ON a.id = aj.article_id " +
                "JOIN Journal j ON aj.journal_id = j.id " +
                "WHERE j.title = ? " +
                "AND EXTRACT(YEAR FROM a.date_completed) IN (?, ?) " +
                "AND ac.citation_year = ?";

        // 计算分母 B：前两年该期刊发表的文章数量
        String sqlB = "SELECT COUNT(*) AS total_articles " +
                "FROM Article a " +
                "JOIN Article_Journal aj ON a.id = aj.article_id " +
                "JOIN Journal j ON aj.journal_id = j.id " +
                "WHERE j.title = ? " +
                "AND EXTRACT(YEAR FROM a.date_completed) IN (?, ?)";

        double impactFactor = 0.0;
        int totalCitations = 0;
        int totalArticles = 0;

        try (Connection conn = dataSource.getConnection()) {
            // 计算 A
            try (PreparedStatement stmtA = conn.prepareStatement(sqlA)) {
                stmtA.setString(1, journal_title);
                stmtA.setInt(2, year - 2);
                stmtA.setInt(3, year - 1);
                stmtA.setInt(4, year);
                ResultSet rsA = stmtA.executeQuery();
                if (rsA.next()) {
                    totalCitations = rsA.getInt("total_citations");
                }
            }

            // 计算 B
            try (PreparedStatement stmtB = conn.prepareStatement(sqlB)) {
                stmtB.setString(1, journal_title);
                stmtB.setInt(2, year - 2);
                stmtB.setInt(3, year - 1);
                ResultSet rsB = stmtB.executeQuery();
                if (rsB.next()) {
                    totalArticles = rsB.getInt("total_articles");
                }
            }

            // 计算影响因子
            if (totalArticles != 0) {
                impactFactor = (double) totalCitations / totalArticles;
            } else {
                log.warn("Total articles is zero for journal: {} in years: {}-{}", journal_title, year - 2, year - 1);
            }

        } catch (SQLException e) {
            log.error("Error calculating Impact Factor for journal: {} in year: {}", journal_title, year, e);
            throw new RuntimeException("Error calculating Impact Factor", e);
        }

        return impactFactor;
    }

    /**
     * 更新期刊名称及其 ID，从指定年份（包含）开始。
     *
     * @param journal  需要更新的期刊对象，仅包含 title 和 id 字段
     * @param year     需要更新的起始年份（包含）
     * @param new_name 新的期刊名称
     * @param new_id   新的期刊 ID
     * @return 更新是否成功
     */
    @Override
    public boolean updateJournalName(Journal journal, int year, String new_name, String new_id) {
        // SQL 语句
        String insertNewJournal = "INSERT INTO Journal (id, title, country, issn, volume, issue) VALUES (?, ?, '', '', '', '')";
        String updateArticleJournal = "UPDATE Article_Journal " +
                "SET journal_id = ? " +
                "WHERE journal_id = ? " +
                "AND article_id IN (" +
                "    SELECT a.id " +
                "    FROM Article a " +
                "    WHERE EXTRACT(YEAR FROM a.date_completed) >= ?" +
                ")";

        Connection conn = null;
        PreparedStatement stmtInsert = null;
        PreparedStatement stmtUpdate = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // 开始事务

            // 插入新的期刊
            stmtInsert = conn.prepareStatement(insertNewJournal);
            stmtInsert.setString(1, new_id);
            stmtInsert.setString(2, new_name);
            stmtInsert.executeUpdate();

            // 更新 Article_Journal 表中的 journal_id
            stmtUpdate = conn.prepareStatement(updateArticleJournal);
            stmtUpdate.setString(1, new_id);
            stmtUpdate.setString(2, journal.getId());
            stmtUpdate.setInt(3, year);
            int rowsUpdated = stmtUpdate.executeUpdate();

            conn.commit(); // 提交事务

            log.info("Updated journal name from {} to {} for {} articles starting from year {}", journal.getTitle(), new_name, rowsUpdated, year);
            return true;
        } catch (SQLException e) {
            log.error("Error updating journal name for journal: {} starting from year: {}", journal.getTitle(), year, e);
            if (conn != null) {
                try {
                    conn.rollback(); // 回滚事务
                } catch (SQLException rollbackEx) {
                    log.error("Error rolling back transaction", rollbackEx);
                }
            }
            return false;
        } finally {
            // 关闭资源
            if (stmtUpdate != null) {
                try {
                    stmtUpdate.close();
                } catch (SQLException e) {
                    log.error("Error closing stmtUpdate", e);
                }
            }
            if (stmtInsert != null) {
                try {
                    stmtInsert.close();
                } catch (SQLException e) {
                    log.error("Error closing stmtInsert", e);
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error closing connection", e);
                }
            }
        }
    }
}

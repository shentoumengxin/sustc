package io.pubmed.service.impl;

import io.pubmed.dto.Journal;
import io.pubmed.service.JournalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

/**
 * JournalService 的实现类，使用引用计数表优化影响因子计算。
 */
@Service
@Slf4j
public class JournalServiceImpl implements JournalService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CitationCountManager citationCountManager;

    /**
     * 计算指定年份的期刊影响因子（Impact Factor）。
     *
     * @param journal_title 需要查询的期刊标题
     * @param year          需要查询的年份
     * @return 指定年份的期刊影响因子
     */
    /**
     * 计算指定年份的期刊影响因子（IF）。
     *
     * IF(year) = 总引用次数 / 前两年发表文章数量
     *
     * @param journal_id 需要查询的期刊标题
     * @param year          需要查询的年份
     * @return 指定年份的期刊影响因子
     */
    @Override
    public double getImpactFactor(String journal_id, int year) {
        String sqlArticles = "SELECT a.id FROM Article a " +
                "JOIN Article_Journal aj ON a.id = aj.article_id " +
                "JOIN Journal j ON aj.journal_id = j.id " +
                "WHERE j.id=? AND EXTRACT(YEAR FROM a.date_completed)::int IN (?, ?)";

        String sqlCountArticles = "SELECT COUNT(*) AS total_articles FROM Article a " +
                "JOIN Article_Journal aj ON a.id = aj.article_id " +
                "JOIN Journal j ON aj.journal_id = j.id " +
                "WHERE j.id = ? AND EXTRACT(YEAR FROM a.date_completed)::int IN (?, ?)";

        double impactFactor = 0.0;
        int totalCitations = 0;
        int totalArticles = 0;

        try (Connection conn = dataSource.getConnection()) {
            // 获取前两年发表的文章ID
            PreparedStatement stmtArticles = conn.prepareStatement(sqlArticles);
            stmtArticles.setString(1, journal_id);
            stmtArticles.setInt(2, year - 2);
            stmtArticles.setInt(3, year - 1);
            ResultSet rsArticles = stmtArticles.executeQuery();

            while (rsArticles.next()) {
                int articleId = rsArticles.getInt("id");
                // 使用 CitationCountManager 获取引用次数
                totalCitations += citationCountManager.getCitationsInYear(articleId, year);
            }

            // 获取前两年发表的文章数量
            PreparedStatement stmtCount = conn.prepareStatement(sqlCountArticles);
            stmtCount.setString(1, journal_id);
            stmtCount.setInt(2, year - 2);
            stmtCount.setInt(3, year - 1);
            ResultSet rsCount = stmtCount.executeQuery();
            if (rsCount.next()) {
                totalArticles = rsCount.getInt("total_articles");
            }

            // 计算影响因子
            if (totalArticles != 0) {
                impactFactor = (double) totalCitations / totalArticles;
            }

        } catch (SQLException e) {

        }

        return impactFactor;
    }

    /**
     * 更新期刊名称及其 ID，从指定年份（包含）开始。
     *
     * @param journal 需要更新的期刊对象，仅包含 title 和 id 字段
     * @param year    需要更新的起始年份（包含）
     * @param new_name 新的期刊名称
     * @param new_id   新的期刊 ID
     * @return 更新是否成功
     */
    @Override
    public boolean updateJournalName(Journal journal, int year, String new_name, String new_id) {
        // SQL 语句
        String insertNewJournal = "INSERT INTO Journal (id, title, country, issn, volume, issue) " +
                "VALUES (?, ?, '', '', '', '')";
        String updateArticleJournal = "UPDATE Article_Journal " +
                "SET journal_id = ? " +
                "WHERE journal_id = ? " +
                "  AND article_id IN ( " +
                "      SELECT a.id " +
                "      FROM Article a " +
                "      WHERE EXTRACT(YEAR FROM a.date_completed)::int >= ? " +
                "  )";
        String deleteInsertedJournal = "DELETE FROM Journal WHERE id = ?"; // 删除插入的期刊
        String revertArticleJournal = "UPDATE Article_Journal " +
                "SET journal_id = ? " +
                "WHERE journal_id = ? " +
                "  AND article_id IN ( " +
                "      SELECT a.id " +
                "      FROM Article a " +
                "      WHERE EXTRACT(YEAR FROM a.date_completed)::int >= ? " +
                "  )";  // 恢复原来的 journal_id

        Connection conn = null;
        PreparedStatement stmtInsert = null;
        PreparedStatement stmtUpdate = null;
        PreparedStatement stmtDelete = null;
        PreparedStatement stmtRevert = null;
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
            stmtUpdate.setString(1, new_id);          // SET journal_id = ?
            stmtUpdate.setString(2, journal.getId()); // WHERE journal_id = ?
            stmtUpdate.setInt(3, year);                // AND EXTRACT(YEAR FROM a.date_completed) >= ?
            int rowsUpdated = stmtUpdate.executeUpdate();

            // 记录更新结果
            boolean updateSuccess = rowsUpdated > 0;

            // 恢复原来的 journal_id
            stmtRevert = conn.prepareStatement(revertArticleJournal);
            stmtRevert.setString(1, journal.getId());  // 恢复为原来的 journal_id
            stmtRevert.setString(2, new_id);           // 旧的 journal_id 是新插入的期刊
            stmtRevert.setInt(3, year);                // 年份保持不变
            stmtRevert.executeUpdate();

            // 删除插入的期刊
            stmtDelete = conn.prepareStatement(deleteInsertedJournal);
            stmtDelete.setString(1, new_id);  // 使用新插入的 journal_id 删除
            stmtDelete.executeUpdate();

            // 提交事务
            conn.commit();

            log.info("Updated journal name from {} to {} for {} articles starting from year {} and deleted the inserted journal",
                    journal.getTitle(), new_name, rowsUpdated, year);

            return updateSuccess; // 返回是否成功更新
        } catch (SQLException e) {
            log.error("Error updating journal name for journal: {} starting from year: {}",
                    journal.getTitle(), year, e);
            if (conn != null) {
                try {
                    conn.rollback(); // 回滚事务
                } catch (SQLException rollbackEx) {
                    log.error("Error rolling back transaction", rollbackEx);
                }
            }
            return false; // 如果发生异常，返回 false
        } finally {
            // 关闭资源
            closeStatement(stmtInsert);
            closeStatement(stmtUpdate);
            closeStatement(stmtDelete);
            closeStatement(stmtRevert);

            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 恢复自动提交
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error closing connection", e);
                }
            }
        }
    }

    private void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("Error closing PreparedStatement", e);
            }
        }
    }


}

package io.pubmed.service.impl;

import io.pubmed.dto.Article;
import io.pubmed.dto.Journal;
import io.pubmed.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.postgresql.core.Oid.UUID;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private CitationCountManager citationCountManager;

    @Override
    public int getArticleCitationsByYear(int id, int year) {
        String sql = "SELECT COUNT(*) FROM article_references ar " +
                "JOIN article a ON ar.article_id = a.id " +
                "WHERE ar.reference_id = ? AND EXTRACT(YEAR FROM a.date_created) = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, year);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            log.error("Error fetching article citations", e);
            throw new RuntimeException("Error fetching article citations", e);
        }

        return 0;  // 如果没有找到引用，则返回0
    }

    @Override
    public double addArticleAndUpdateIF(Article article) {
        try {
            insertArticleAndJournal(article);
            if (article.getReferences() != null) {
                for (int i = 0; i < article.getReferences().length; i++) {
                    int id = Integer.parseInt(article.getReferences()[i]);
                    citationCountManager.incrementCitationCount(id, 1);
                }
            }
            String sqlArticles = "SELECT a.id FROM Article a " +
                    "JOIN Article_Journal aj ON a.id = aj.article_id " +
                    "JOIN Journal j ON aj.journal_id = j.id " +
                    "WHERE j.id = ? AND EXTRACT(YEAR FROM a.date_completed) IN (?, ?)";

            String sqlCountArticles = "SELECT COUNT(*) AS total_articles FROM Article a " +
                    "JOIN Article_Journal aj ON a.id = aj.article_id " +
                    "JOIN Journal j ON aj.journal_id = j.id " +
                    "WHERE j.title = ? AND EXTRACT(YEAR FROM a.date_completed) IN (?, ?)";

            double impactFactor = 0.0;
            int totalCitations = 0;
            int totalArticles = 0;

            Connection conn = dataSource.getConnection();
            // 获取前两年发表的文章ID
            Journal journal = article.getJournal();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(article.getCompleted());
            int year = calendar.get(Calendar.YEAR);
            PreparedStatement stmtArticles = conn.prepareStatement(sqlArticles);
            stmtArticles.setString(1, journal.getTitle());
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
            stmtCount.setString(1, journal.getTitle());
            stmtCount.setInt(2, year - 2);
            stmtCount.setInt(3, year - 1);
            ResultSet rsCount = stmtCount.executeQuery();
            if (rsCount.next()) {
                totalArticles = rsCount.getInt("total_articles");
            }

            // 计算影响因子
            if (totalArticles != 0) {
                impactFactor = (double) totalCitations / totalArticles;
            } else {
                log.warn("前两年发表文章数量为零，无法计算影响因子。");
            }
            deleteArticleAndAssociatedData(article);
            return impactFactor;

        } catch (SQLException e) {
            throw new RuntimeException(e);

        }

    }

    //自己实现的代码
    private void insertArticleAndJournal(Article article) throws SQLException {
        // Step 1: Insert the article into the Article table
        String insertArticleSQL = "INSERT INTO Article (id, title, pub_model, date_created, date_completed) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement articleStmt = dataSource.getConnection().prepareStatement(insertArticleSQL)) {
            // Set parameters for the Article table
            articleStmt.setInt(1, article.getId());
            articleStmt.setString(2, article.getTitle());
            articleStmt.setString(3, article.getPub_model());
            articleStmt.setDate(4, new java.sql.Date(article.getCreated().getTime()));
            if (article.getCompleted() != null) {
                articleStmt.setDate(5, new java.sql.Date(article.getCompleted().getTime()));
            } else {
                articleStmt.setNull(5, Types.DATE); // If the completed date is null
            }

            // Execute insert into Article table
            articleStmt.executeUpdate();
        }

        // Step 2: Insert the article-journal relation into the Article_Journal table
        insertArticleJournal(article);
    }

    /**
     * Insert the article-journal relation into the Article_Journal table.
     *
     * @param article the article to be associated with a journal
     * @throws SQLException if any SQL error occurs
     */
    private void insertArticleJournal(Article article) throws SQLException {
        String checkJournalSQL = "SELECT id FROM Journal WHERE id= ?";
        String journalId = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkJournalSQL)) {
            checkStmt.setString(1, article.getJournal().getId());  // 使用期刊标题查询

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // 如果期刊存在，获取现有的 journal_id
                journalId = rs.getString("id");
            } else {
                // 如果期刊不存在，插入新的期刊
                String insertJournalSQL = "INSERT INTO Journal (id, country, issn, title, volume, issue) VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = conn.prepareStatement(insertJournalSQL)) {
                    insertStmt.setString(1, article.getJournal().getId());  // 设置新的 journal_id
                    insertStmt.setString(2, "");
                    insertStmt.setString(3,"");
                    insertStmt.setString(4, article.getJournal().getTitle());
                    if (article.getJournal().getIssue()!=null) {
                        if (article.getJournal().getIssue().getVolume()!=null) {
                            insertStmt.setString(5, article.getJournal().getIssue().getVolume());
                        }else{
                            insertStmt.setString(5,"");
                        }
                        if (article.getJournal().getIssue().getIssue()!=null) {
                            insertStmt.setString(6, article.getJournal().getIssue().getIssue());
                        }else{
                            insertStmt.setString(6, "");
                        }
                    }else{
                        insertStmt.setString(5, "");
                        insertStmt.setString(6, "");
                    }
                    insertStmt.executeUpdate();
                }
            }

            String insertArticleJournalSQL = "INSERT INTO Article_Journal (journal_id, article_id) VALUES (?, ?)";

            // Assuming the journal object is part of the article object
            try (PreparedStatement stmt = dataSource.getConnection().prepareStatement(insertArticleJournalSQL)) {
                // Set parameters for the Article_Journal table
                stmt.setString(1, article.getJournal().getId());  // Set the journal ID
                stmt.setInt(2, article.getId()); // Set the article ID
                // Execute insert into Article_Journal table
                stmt.executeUpdate();
            }
        }
    }

    public void deleteArticleAndAssociatedData(Article article) throws SQLException {
        // Step 1: Delete the article-journal relation from the Article_Journal table
        String deleteArticleJournalSQL = "DELETE FROM Article_Journal WHERE article_id = ?";
        try (PreparedStatement deleteStmt = dataSource.getConnection().prepareStatement(deleteArticleJournalSQL)) {
            deleteStmt.setInt(1, article.getId());
            deleteStmt.executeUpdate();
        }

        // Step 2: Delete the article from the Article table
        String deleteArticleSQL = "DELETE FROM Article WHERE id = ?";
        try (PreparedStatement deleteStmt = dataSource.getConnection().prepareStatement(deleteArticleSQL)) {
            deleteStmt.setInt(1, article.getId());
            deleteStmt.executeUpdate();
        }
        String journalId=article.getJournal().getId();
        if (journalId != null) {
            String checkJournalUsageSQL = "SELECT COUNT(*) FROM Article_Journal WHERE journal_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkJournalUsageSQL)) {
                checkStmt.setString(1, journalId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    // If no other articles are using this journal, delete the journal
                    String deleteJournalSQL = "DELETE FROM Journal WHERE id = ?";
                    try (PreparedStatement deleteJournalStmt = conn.prepareStatement(deleteJournalSQL)) {
                        deleteJournalStmt.setString(1, journalId);
                        deleteJournalStmt.executeUpdate();
                    }
                }
            }
        }
    }

}

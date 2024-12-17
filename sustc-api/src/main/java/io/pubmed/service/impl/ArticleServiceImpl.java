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
@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private DataSource dataSource;
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
    @Override//还未写完
    public double addArticleAndUpdateIF(Article article) {
return 0;
    }
    //自己实现的代码
    public void insertArticleAndJournal(Article article) throws SQLException {
        // Step 1: Insert the article into the Article table
        String insertArticleSQL = "INSERT INTO Article (id, title, pub_model, date_created, date_completed) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement articleStmt = dataSource.getConnection().prepareStatement(insertArticleSQL)) {
            articleStmt.setInt(1, article.getId());
            articleStmt.setString(2, article.getTitle());
            articleStmt.setString(3, article.getPub_model());
            articleStmt.setDate(4, new java.sql.Date(article.getCreated().getTime()));

            if (article.getCompleted() != null) {
                articleStmt.setDate(5, new java.sql.Date(article.getCompleted().getTime()));
            } else {
                articleStmt.setNull(5, Types.DATE);
            }

            articleStmt.executeUpdate();
        }

        // Step 2: Insert the journal into the Journal table (if not exists)
        String journalId = insertOrGetJournal(article.getJournal());

        // Step 3: Insert the article-journal relation into the Article_Journal table
        String insertArticleJournalSQL = "INSERT INTO Article_Journal (journal_id, article_id) VALUES (?, ?)";
        try (PreparedStatement articleJournalStmt = dataSource.getConnection().prepareStatement(insertArticleJournalSQL)) {
            articleJournalStmt.setString(1, journalId);
            articleJournalStmt.setInt(2, article.getId());
            articleJournalStmt.executeUpdate();
        }
    }

    /**
     * Insert the journal into the Journal table if it does not exist, or retrieve its ID if it exists.
     * @param journal the journal to be inserted
     * @return the journal ID
     * @throws SQLException if any SQL error occurs
     */
    private String insertOrGetJournal(Journal journal) throws SQLException {
        // Check if the journal exists
        String checkJournalSQL = "SELECT id FROM Journal WHERE id = ?";
        try (PreparedStatement checkStmt = dataSource.getConnection().prepareStatement(checkJournalSQL)) {
            checkStmt.setString(1, journal.getId());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                return rs.getString("id"); // Return existing journal ID
            }
        }

        // If the journal does not exist, insert a new record
        String insertJournalSQL = "INSERT INTO Journal (id, country, issn, title, volume, issue) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insertStmt = dataSource.getConnection().prepareStatement(insertJournalSQL)) {
            insertStmt.setString(1, journal.getId());
            insertStmt.setString(2, journal.getCountry());
            insertStmt.setString(3, journal.getIssn());
            insertStmt.setString(4, journal.getTitle());
            insertStmt.setString(5, journal.getIssue().getVolume());
            insertStmt.setString(6, journal.getIssue().getIssue());
            insertStmt.executeUpdate();
        }

        return journal.getId(); // Return newly created journal ID
    }
}

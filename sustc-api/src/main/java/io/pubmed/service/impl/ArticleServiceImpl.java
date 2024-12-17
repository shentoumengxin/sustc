package io.pubmed.service.impl;

import io.pubmed.dto.Article;
import io.pubmed.dto.Journal;
import io.pubmed.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

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
        String insertSql = "INSERT INTO article (id,title, pub_model, date_created, date_completed) " +
                "VALUES (?, ?, ?, ?) RETURNING id;" +
                "                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +--";
        String DeleteSql="delete from article where id=?";
       return 0;
    }

    // 获取与文章相关的期刊
    private Journal getJournalByArticleId(int articleId) {
        String sql = "SELECT j.id, j.title FROM journal j " +
                "JOIN article_journal aj ON j.id = aj.journal_id " +
                "WHERE aj.article_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Journal journal = new Journal();
                journal.setId(rs.getString("id"));
                journal.setTitle(rs.getString("title"));
                return journal;
            }

        } catch (SQLException e) {
            log.error("Error fetching journal by article ID", e);
        }

        return null;
    }
}

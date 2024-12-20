package io.pubmed.service.impl;

import io.pubmed.dto.Author;
import io.pubmed.service.AuthorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@Slf4j
public class AuthorServiceImpl implements AuthorService {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private CitationCountManager citationCountManager;

    @Override
    public int[] getArticlesByAuthorSortedByCitations(Author author) {
        String sql = "SELECT a.id AS article_id " +
                "FROM Article a " +
                "JOIN Article_Authors aa ON a.id = aa.article_id " +
                "JOIN Authors auth ON aa.author_id = auth.author_id " +
                "WHERE auth.fore_name = ? " +
                "AND auth.last_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, author.getFore_name());
            stmt.setString(2, author.getLast_name());
            ResultSet rs = stmt.executeQuery();
            ArrayList<Integer> a = new ArrayList<>();
            while (rs.next()) {
                int articleId = rs.getInt("article_id");
                a.add(citationCountManager.getCitationCount(articleId));
            }
            int[] result = new int[a.size()];
            for (int i = 0; i < a.size(); i++) {
                result[i]=a.get(i);
            }
            Arrays.sort(result);
            // 反转数组使其变为降序
            for (int i = 0; i < result.length / 2; i++) {
                int temp = result[i];
                result[i] = result[result.length - 1 - i];
                result[result.length - 1 - i] = temp;
            }
            return result;
        } catch (SQLException e) {
            System.out.println(author);
            log.error("Error fetching article citations", e);
            throw new RuntimeException("Error fetching article citations", e);
        }
    }

    @Override
    public String getJournalWithMostArticlesByAuthor(Author author) {
        String sql = "SELECT j.title AS journal_title " +
                "FROM Journal j " +
                "JOIN Article_Journal aj ON j.id = aj.journal_id " +
                "JOIN Article_Authors aa ON aj.article_id = aa.article_id " +
                "JOIN Authors a ON aa.author_id = a.author_id " +
                "WHERE a.fore_name = ? " +
                "  AND a.last_name = ? " +
                "GROUP BY j.title " +
                "ORDER BY COUNT(aj.article_id) DESC " +
                "LIMIT 1";

        // 执行查询并获取结果
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置查询参数
            stmt.setString(1, author.getFore_name());
            stmt.setString(2, author.getLast_name());

            // 执行查询
            ResultSet rs = stmt.executeQuery();

            // 如果查询结果有记录，则返回期刊名称
            if (rs.next()) {
                return rs.getString("journal_title");
            } else {
                return null; // 如果没有找到该作者的期刊，则返回 null
            }
        } catch (SQLException e) {
            log.error("Error fetching article citations", e);
            throw new RuntimeException("Error fetching article citations", e);
        }
    }

    /**
     * Find the minimum number of articles that two authors need to be linked by citations.
     *
     * @param A the first author
     * @param E the second author
     * @return the number of required articles, if no connection exists return -1
     */
    public int getMinArticlesToLinkAuthors(Author A, Author E) {
        // Step 1: Get articles of author A
        try {
            Set<Integer> authorAArticles = getAuthorArticles(A);
            Set<Integer> authorEArticles = getAuthorArticles(E);

            // Step 2: BFS initialization
            Queue<int[]> queue = new LinkedList<>();
            Set<Integer> visitedArticles = new HashSet<>();

            // Add all of A's articles to the queue with depth 0
            for (int articleId : authorAArticles) {
                queue.offer(new int[]{articleId, 0}); // {article_id, depth}
                visitedArticles.add(articleId);
            }

            // Step 3: BFS for the shortest path
            while (!queue.isEmpty()) {
                int[] current = queue.poll();
                int currentArticleId = current[0];
                int currentDepth = current[1];

                // Step 4: Check if current article is written by author E
                if (isArticleWrittenByAuthor(currentArticleId, E)) {
                    return currentDepth; // Found, return the number of articles (depth)
                }

                // Step 5: Get references of the current article
                Set<Integer> references = getArticleReferences(currentArticleId);
                for (int refId : references) {
                    // If the referenced article is not visited yet, add it to the queue
                    if (!visitedArticles.contains(refId)) {
                        queue.offer(new int[]{refId, currentDepth + 1});
                        visitedArticles.add(refId);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching article citations", e);
            throw new RuntimeException("Error fetching article citations", e);
        }

        // If no connection found
        return -1;
    }

    // Helper method to get all articles of a given author
    private Set<Integer> getAuthorArticles(Author author) throws SQLException {
        Set<Integer> articles = new HashSet<>();
        String sql = "SELECT a.id FROM Article a JOIN Article_Authors aa ON a.id = aa.article_id " +
                "JOIN Authors auth ON aa.author_id = auth.author_id WHERE auth.fore_name = ? " +
                "AND auth.last_name = ? AND auth.initials = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, author.getFore_name());
            stmt.setString(2, author.getLast_name());
            stmt.setString(3, author.getInitials());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(rs.getInt("id"));
            }
        }
        return articles;
    }

    // Helper method to check if an article is written by a specific author
    private boolean isArticleWrittenByAuthor(int articleId, Author author) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Article_Authors aa JOIN Authors auth ON aa.author_id = auth.author_id " +
                "WHERE aa.article_id = ? AND auth.fore_name = ? AND auth.last_name = ? AND auth.initials = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            stmt.setString(2, author.getFore_name());
            stmt.setString(3, author.getLast_name());
            stmt.setString(4, author.getInitials());
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Helper method to get all references of a given article
    private Set<Integer> getArticleReferences(int articleId) throws SQLException {
        Set<Integer> references = new HashSet<>();
        String sql = "SELECT ar.reference_id FROM article_references ar WHERE ar.article_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                references.add(rs.getInt("reference_id"));
            }
        }
        return references;
    }
}


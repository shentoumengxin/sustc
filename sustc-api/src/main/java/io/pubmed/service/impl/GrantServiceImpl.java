package io.pubmed.service.impl;

import io.pubmed.service.GrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GrantServiceImpl implements GrantService {
    @Autowired
    private DataSource dataSource;

    @Override
    public int[] getCountryFundPapers(String country) {
        // SQL 查询：查找与给定国家相关的所有资助文章的 pmid
        String sql = "SELECT a.id as pmid " +
                "FROM Article a " +
                "JOIN Article_Grants ag ON a.id = ag.article_id " +
                "JOIN Grant_info g ON ag.grant_id = g.id " +
                "WHERE g.country = ?";

        // 用于存储查询结果的列表
        List<Integer> pmidList = new ArrayList<>();

        // 执行数据库查询
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置查询参数，传入国家
            stmt.setString(1, country);

            // 执行查询
            ResultSet rs = stmt.executeQuery();

            // 处理查询结果
            while (rs.next()) {
                pmidList.add(rs.getInt("pmid"));  // 获取 pmid 并添加到列表
            }

        } catch (SQLException e) {
            log.error("Error fetching funded papers by country", e);
            throw new RuntimeException("Error fetching funded papers by country", e);
        }

        // 将 List 转换为 int[] 数组并返回
        return pmidList.stream().mapToInt(Integer::intValue).toArray();
    }
}

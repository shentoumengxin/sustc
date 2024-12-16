package io.pubmed.dto;

import lombok.Data;

/**
 * 实体类，映射到 Article_Citation_Count 表。
 */
@Data
public class ArticleCitationCount {
    private int articleId;        // 文章ID，对应 Article 表的 id
    private int citationCount;    // 引用计数
}

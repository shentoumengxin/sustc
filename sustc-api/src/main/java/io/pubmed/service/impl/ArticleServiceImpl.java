package io.pubmed.service.impl;

import io.pubmed.dto.Article;
import io.pubmed.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private DataSource dataSource;

    @Override
    public int getArticleCitationsByYear(int id, int year) {
        return 0;
    }

    @Override
    public double addArticleAndUpdateIF(Article article) {
        return 0;
    }
}

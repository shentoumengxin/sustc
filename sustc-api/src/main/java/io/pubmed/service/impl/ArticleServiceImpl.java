package io.pubmed.service.impl;

import io.pubmed.dto.Article;
import io.pubmed.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {
    @Override
    public int getArticleCitationsByYear(int id, int year) {
        return 0;
    }

    @Override
    public double addArticleAndUpdateIF(Article article) {
        return 0;
    }
}

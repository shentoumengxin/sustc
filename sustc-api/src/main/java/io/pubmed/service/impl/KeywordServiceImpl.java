package io.pubmed.service.impl;

import io.pubmed.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service

public class KeywordServiceImpl implements KeywordService {
    @Autowired
    private DataSource dataSource;

    @Override
    public int[] getArticleCountByKeywordInPastYears(String keyword) {
        return new int[0];
    }
}

package io.pubmed.service.impl;

import io.pubmed.service.KeywordService;
import org.springframework.stereotype.Service;

@Service

public class KeywordServiceImpl implements KeywordService {
    @Override
    public int[] getArticleCountByKeywordInPastYears(String keyword) {
        return new int[0];
    }
}

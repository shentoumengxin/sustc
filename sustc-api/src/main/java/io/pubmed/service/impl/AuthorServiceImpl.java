package io.pubmed.service.impl;

import io.pubmed.dto.Author;
import io.pubmed.service.AuthorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthorServiceImpl implements AuthorService {
    @Override
    public int[] getArticlesByAuthorSortedByCitations(Author author) {
        return new int[0];
    }

    @Override
    public String getJournalWithMostArticlesByAuthor(Author author) {
        return "";
    }

    @Override
    public int getMinArticlesToLinkAuthors(Author A, Author E) {
        return 0;
    }
}

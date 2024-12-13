package io.pubmed.service.impl;

import io.pubmed.service.GrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service

public class GrantServiceImpl implements GrantService {
    @Autowired
    private DataSource dataSource;

    @Override
    public int[] getCountryFundPapers(String country) {
        return new int[0];
    }
}

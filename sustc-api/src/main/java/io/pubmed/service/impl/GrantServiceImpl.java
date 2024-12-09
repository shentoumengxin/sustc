package io.pubmed.service.impl;

import io.pubmed.service.GrantService;
import org.springframework.stereotype.Service;

@Service

public class GrantServiceImpl implements GrantService {
    @Override
    public int[] getCountryFundPapers(String country) {
        return new int[0];
    }
}

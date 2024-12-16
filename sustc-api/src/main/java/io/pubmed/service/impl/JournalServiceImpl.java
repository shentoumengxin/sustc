package io.pubmed.service.impl;

import io.pubmed.dto.Journal;
import io.pubmed.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service

public class JournalServiceImpl implements JournalService {
    @Autowired
    private DataSource dataSource;


    @Override
    public double getImpactFactor(String journal_title, int year) {
        return 0;
    }

    @Override
    public boolean updateJournalName(Journal journal, int year, String new_name, String new_id) {
        return false;
    }
}

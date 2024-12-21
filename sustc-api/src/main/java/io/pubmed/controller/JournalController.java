package io.pubmed.controller;

import io.pubmed.dto.Journal;
import io.pubmed.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journals")
public class JournalController {

    @Autowired
    private JournalService journalService;

    /**
     * 获取期刊在特定年份的影响因子
     * @param journalId 期刊ID
     * @param year 查询年份
     * @return 期刊影响因子
     */
    @GetMapping("/{journalId}/impact-factor")
    public double getImpactFactor(@PathVariable String journalId, @RequestParam int year) {
        return journalService.getImpactFactor(journalId, year);
    }

    /**
     * 更新期刊的名称和ID
     * @param journal 期刊对象
     * @param year 更新年份
     * @param newName 新名称
     * @param newId 新ID
     * @return 更新成功与否
     */
    @PostMapping("/update")
    public boolean updateJournalName(@RequestBody Journal journal,
                                     @RequestParam int year,
                                     @RequestParam String newName,
                                     @RequestParam String newId) {
        return journalService.updateJournalName(journal, year, newName, newId);
    }
}

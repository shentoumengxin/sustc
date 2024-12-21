package io.pubmed.controller;

import io.pubmed.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * KeywordService 控制器，处理与关键字相关的请求。
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    @Autowired
    private KeywordService keywordService;

    /**
     * 根据给定关键字查询过去一年的发表文章数量，并按年份降序排列。
     *
     * @param keyword 需要查询的关键字
     * @return 包含每年文章数量的数组
     */
    @GetMapping("/count")
    public int[] getArticleCountByKeywordInPastYears(@RequestParam String keyword) {
        return keywordService.getArticleCountByKeywordInPastYears(keyword);
    }
}

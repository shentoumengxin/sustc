package io.pubmed.controller;

import io.pubmed.dto.Article;
import io.pubmed.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    /**
     * 获取指定文章在特定年份的引用次数
     * @param id 文章ID
     * @param year 需要查询的年份
     * @return 文章在指定年份的引用次数
     */
    @GetMapping("/{id}/citations")
    public int getArticleCitationsByYear(@PathVariable int id, @RequestParam int year) {
        return articleService.getArticleCitationsByYear(id, year);
    }

    /**
     * 添加文章并更新期刊影响因子
     * @param article 文章对象
     * @return 更新后的期刊影响因子
     */
    @PostMapping("/add")
    public double addArticleAndUpdateIF(@RequestBody Article article) {
        return articleService.addArticleAndUpdateIF(article);
    }
}

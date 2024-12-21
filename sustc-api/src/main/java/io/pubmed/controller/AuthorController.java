package io.pubmed.controller;

import io.pubmed.dto.Author;
import io.pubmed.service.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    @Autowired
    private AuthorService authorService;

    /**
     * 按引用次数对作者的文章进行排序
     * @param   fore_name, last_name
     * @return 排序后的文章ID列表
     */
    @GetMapping("/{authorId}/articles/sorted")
    public int[] getArticlesByAuthorSortedByCitations(@PathVariable String fore_name,String last_name) {
        Author author = new Author(); // 创建并设置作者对象
        author.setFore_name(fore_name);
        author.setLast_name(last_name);
        return authorService.getArticlesByAuthorSortedByCitations(author);
    }

    /**
     * 查询作者发表最多文章的期刊
     * @param   fore_name, last_name
     * @return 期刊名称
     */
    @GetMapping("/{authorId}/journal")
    public String getJournalWithMostArticlesByAuthor(@PathVariable String fore_name,String last_name) {
        Author author = new Author(); // 创建并设置作者对象
        author.setFore_name(fore_name);
        author.setLast_name(last_name);
        return authorService.getJournalWithMostArticlesByAuthor(author);
    }
}

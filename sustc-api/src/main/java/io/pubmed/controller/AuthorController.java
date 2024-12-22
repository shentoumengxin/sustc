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
    @GetMapping("/articles/sorted/{fore_name}/{last_name}")
    public int[] getArticlesByAuthorSortedByCitations(
            @PathVariable String fore_name, // 获取路径中的 fore_name
            @PathVariable String last_name) { // 获取路径中的 last_name
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
    @GetMapping("/journal/{fore_name}/{last_name}")
    public String getJournalWithMostArticlesByAuthor(
            @PathVariable String fore_name, // 获取路径中的 fore_name
            @PathVariable String last_name) { // 获取路径中的 last_name
        Author author = new Author(); // 创建并设置作者对象
        author.setFore_name(fore_name);
        author.setLast_name(last_name);
        return authorService.getJournalWithMostArticlesByAuthor(author);
    }

    /**
     * 查找两个作者通过引用链接所需的最小文章数
     * @param fore_nameA, last_nameA, fore_nameB, last_nameB
     * @return 最小的文章数，如果没有连接则返回 -1
     */
    @GetMapping("/link/{fore_nameA}/{last_nameA}/{fore_nameB}/{last_nameB}")
    public int getMinArticlesToLinkAuthors(
            @PathVariable String fore_nameA,
            @PathVariable String last_nameA,
            @PathVariable String fore_nameB,
            @PathVariable String last_nameB) {

        Author authorA = new Author();
        authorA.setFore_name(fore_nameA);
        authorA.setLast_name(last_nameA);

        Author authorB = new Author();
        authorB.setFore_name(fore_nameB);
        authorB.setLast_name(last_nameB);

        return authorService.getMinArticlesToLinkAuthors(authorA, authorB);
    }
}

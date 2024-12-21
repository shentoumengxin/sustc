package io.pubmed.controller;

import io.pubmed.service.GrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * GrantService 控制器，处理与资助相关的请求。
 */
@RestController
@RequestMapping("/api/grants")
public class GrantController {

    @Autowired
    private GrantService grantService;

    /**
     * 根据给定国家查询资助文章的 pmid 列表
     *
     * @param country 国家名称
     * @return 资助文章的 pmid 列表
     */
    @GetMapping("/country/{country}/papers")
    public int[] getCountryFundPapers(@PathVariable String country) {
        return grantService.getCountryFundPapers(country);
    }
}

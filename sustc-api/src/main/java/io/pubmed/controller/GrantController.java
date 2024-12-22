package io.pubmed.controller;

import io.pubmed.service.GrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Object> getCountryFundPapers(
            @PathVariable String country,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        // 获取总数据
        int[] allPapers = grantService.getCountryFundPapers(country);

        // 计算分页信息
        int total = allPapers.length;
        int totalPages = (total + size - 1) / size;

        // 计算当前页的数据范围
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        // 提取当前页的数据
        int[] pageData = Arrays.copyOfRange(allPapers, start, end);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("papers", pageData);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);
        result.put("pageSize", size);

        return result;
    }
}

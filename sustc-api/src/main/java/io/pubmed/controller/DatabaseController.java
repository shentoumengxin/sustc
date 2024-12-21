package io.pubmed.controller;

import io.pubmed.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    /**
     * 获取项目组成员的学号
     * @return 项目组成员学号列表
     */
    @GetMapping("/group-members")
    public List<Integer> getGroupMembers() {
        return databaseService.getGroupMembers();
    }

    /**
     * 导入数据到数据库
     * @param dataPath 数据文件路径
     */
    @PostMapping("/import")
    public void importData(@RequestParam String dataPath) {
        databaseService.importData(dataPath);
    }

    /**
     * 清空数据库中的所有表
     */
    @PostMapping("/truncate")
    public void truncateDatabase() {
        databaseService.truncate();
    }
}

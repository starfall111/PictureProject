package org.example.server.scheduled;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.entity.Category;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.Tag;
import org.example.server.service.CategoryService;
import org.example.server.service.PictureService;
import org.example.server.service.TagService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类和标签计数同步定时任务
 *
 * @author Zou
 */
@Slf4j
@Component
public class CountSyncScheduled {

    @Resource(name = "dbPictureService")
    private PictureService pictureService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private TagService tagService;

    /**
     * 每小时同步一次分类和标签的 count
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void syncCount() {
        log.info("开始同步分类和标签计数");

        // 查询所有未删除的图片
        List<Picture> pictures = pictureService.list();

        // 统计分类计数
        Map<Long, Integer> categoryCountMap = new HashMap<>();
        // 统计标签计数
        Map<String, Integer> tagCountMap = new HashMap<>();

        for (Picture picture : pictures) {
            // 分类计数
            Long categoryId = picture.getCategoryId();
            if (categoryId != null && categoryId > 0) {
                categoryCountMap.merge(categoryId, 1, Integer::sum);
            }

            // 标签计数（解析 JSON）
            String tagsJson = picture.getTags();
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    List<String> tagNames = JSONUtil.toList(tagsJson, String.class);
                    for (String tagName : tagNames) {
                        tagCountMap.merge(tagName, 1, Integer::sum);
                    }
                } catch (Exception e) {
                    log.warn("解析图片标签 JSON 失败: {}", tagsJson);
                }
            }
        }

        // 更新分类 count
        List<Category> categories = categoryService.list();
        for (Category category : categories) {
            Integer count = categoryCountMap.getOrDefault(category.getId(), 0);
            category.setCount(count);
        }
        categoryService.updateBatchById(categories);

        // 更新标签 count（仅更新 tag 表中已存在的标签）
        List<Tag> tags = tagService.list();
        for (Tag tag : tags) {
            Integer count = tagCountMap.getOrDefault(tag.getName(), 0);
            tag.setCount(count);
        }
        tagService.updateBatchById(tags);

        log.info("分类和标签计数同步完成，分类数: {}，标签数: {}", categories.size(), tags.size());
    }
}

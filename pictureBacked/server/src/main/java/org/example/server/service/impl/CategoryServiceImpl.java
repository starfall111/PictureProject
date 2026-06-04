package org.example.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.category.CategoryQueryDTO;
import org.example.pojo.entity.Category;
import org.example.server.service.CategoryService;
import org.example.server.mapper.CategoryMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Zou
 * @description 针对表【category(分类)】的数据库操作Service实现
 * @createDate 2026-05-17 15:54:12
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    private static final String CATEGORY_LIST_KEY = "category:list";
    private static final String CATEGORY_PAGE_KEY_PREFIX = "category:page:";
    private static final String CATEGORY_DETAIL_KEY_PREFIX = "category:detail:";

    @Override
    public List<Category> listCategoryCache() {
        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        String cacheResult = redisCacheUtil.getWithLock(CATEGORY_LIST_KEY, ttlSeconds, () -> {
            QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderBy(true, false, "count");
            List<Category> list = this.list(queryWrapper);
            return list.isEmpty() ? null : JSONUtil.toJsonStr(list);
        });
        if (cacheResult == null) {
            return List.of();
        }
        return JSONUtil.toList(cacheResult, Category.class);
    }

    @Override
    public Page<Category> queryCategoryPageCache(CategoryQueryDTO categoryQueryDTO) {
        String key = JSONUtil.toJsonStr(categoryQueryDTO);
        String hashKey = CATEGORY_PAGE_KEY_PREFIX + DigestUtils.md5DigestAsHex(key.getBytes());

        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        String cacheResult = redisCacheUtil.getWithLock(hashKey, ttlSeconds, () -> {
            long current = categoryQueryDTO.getCurrent();
            long pageSize = categoryQueryDTO.getPageSize();

            QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
            String name = categoryQueryDTO.getName();
            if (StrUtil.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }

            String sortField = categoryQueryDTO.getSortField();
            if (StrUtil.isNotBlank(sortField)) {
                queryWrapper.orderBy(true, "ascend".equals(categoryQueryDTO.getSortOrder()), sortField);
            } else {
                queryWrapper.orderByDesc("id");
            }

            Page<Category> page = this.page(new Page<>(current, pageSize), queryWrapper);
            return JSONUtil.toJsonStr(page);
        });
        if (cacheResult == null) {
            return new Page<>(categoryQueryDTO.getCurrent(), categoryQueryDTO.getPageSize());
        }
        return JSONUtil.toBean(cacheResult, Page.class);
    }

    @Override
    public Category getCategoryByIdCache(Long id) {
        String cacheKey = CATEGORY_DETAIL_KEY_PREFIX + id;
        int ttlSeconds = 300 + RandomUtil.randomInt(0, 300);

        String cacheResult = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            Category category = this.getById(id);
            return category == null ? null : JSONUtil.toJsonStr(category);
        });
        if (cacheResult == null) {
            return null;
        }
        return JSONUtil.toBean(cacheResult, Category.class);
    }

    @Override
    public void clearCategoryCache() {
        stringRedisTemplate.delete(CATEGORY_LIST_KEY);
        redisCacheUtil.deleteByPattern(CATEGORY_PAGE_KEY_PREFIX + "*");
        redisCacheUtil.deleteByPattern(CATEGORY_DETAIL_KEY_PREFIX + "*");
    }
}

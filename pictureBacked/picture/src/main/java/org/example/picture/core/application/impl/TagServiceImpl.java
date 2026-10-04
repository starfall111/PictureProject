package org.example.picture.core.application.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.core.interfaces.dto.TagQueryDTO;
import org.example.picture.core.domain.model.Tag;
import org.example.picture.core.application.TagService;
import org.example.picture.core.infrastructure.persistence.TagMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Zou
 * @description 针对表【tag(标签)】的数据库操作Service实现
 * @createDate 2026-05-17 15:54:03
 */
@Slf4j
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    private static final String TAG_LIST_KEY = "tag:list";
    private static final String TAG_PAGE_KEY_PREFIX = "tag:page:";
    private static final String TAG_DETAIL_KEY_PREFIX = "tag:detail:";

    @Override
    public List<Tag> listTagCache() {
        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        String cacheResult = redisCacheUtil.getWithLock(TAG_LIST_KEY, ttlSeconds, () -> {
            QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderBy(true, false, "count");
            List<Tag> list = this.list(queryWrapper);
            return list.isEmpty() ? null : JSONUtil.toJsonStr(list);
        });
        if (cacheResult == null) {
            return List.of();
        }
        return JSONUtil.toList(cacheResult, Tag.class);
    }

    @Override
    public Page<Tag> queryTagPageCache(TagQueryDTO tagQueryDTO) {
        String key = JSONUtil.toJsonStr(tagQueryDTO);
        String hashKey = TAG_PAGE_KEY_PREFIX + DigestUtils.md5DigestAsHex(key.getBytes());

        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        String cacheResult = redisCacheUtil.getWithLock(hashKey, ttlSeconds, () -> {
            long current = tagQueryDTO.getCurrent();
            long pageSize = tagQueryDTO.getPageSize();

            QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
            String name = tagQueryDTO.getName();
            if (StrUtil.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }

            String sortField = tagQueryDTO.getSortField();
            if (StrUtil.isNotBlank(sortField)) {
                queryWrapper.orderBy(true, "ascend".equals(tagQueryDTO.getSortOrder()), sortField);
            } else {
                queryWrapper.orderByDesc("id");
            }

            Page<Tag> page = this.page(new Page<>(current, pageSize), queryWrapper);
            return JSONUtil.toJsonStr(page);
        });
        if (cacheResult == null) {
            return new Page<>(tagQueryDTO.getCurrent(), tagQueryDTO.getPageSize());
        }
        return JSONUtil.toBean(cacheResult, Page.class);
    }

    @Override
    public Tag getTagByIdCache(Long id) {
        String cacheKey = TAG_DETAIL_KEY_PREFIX + id;
        int ttlSeconds = 300 + RandomUtil.randomInt(0, 300);

        String cacheResult = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            Tag tag = this.getById(id);
            return tag == null ? null : JSONUtil.toJsonStr(tag);
        });
        if (cacheResult == null) {
            return null;
        }
        return JSONUtil.toBean(cacheResult, Tag.class);
    }

    @Override
    public void clearTagCache() {
        stringRedisTemplate.delete(TAG_LIST_KEY);
        redisCacheUtil.deleteByPattern(TAG_PAGE_KEY_PREFIX + "*");
        redisCacheUtil.deleteByPattern(TAG_DETAIL_KEY_PREFIX + "*");
    }
}

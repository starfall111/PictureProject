package org.example.picture.core.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.core.interfaces.dto.TagQueryDTO;
import org.example.picture.core.domain.model.Tag;

import java.util.List;

/**
* @author Zou
* @description 针对表【tag(标签)】的数据库操作Service
* @createDate 2026-05-17 15:54:03
*/
public interface TagService extends IService<Tag> {

    /**
     * 获取标签列表（带缓存）
     */
    List<Tag> listTagCache();

    /**
     * 分页查询标签列表（带缓存）
     */
    Page<Tag> queryTagPageCache(TagQueryDTO tagQueryDTO);

    /**
     * 根据 id 获取标签（带缓存）
     */
    Tag getTagByIdCache(Long id);

    /**
     * 清除标签相关缓存
     */
    void clearTagCache();
}

package org.example.picture.core.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.core.interfaces.dto.CategoryQueryDTO;
import org.example.picture.core.domain.model.Category;

import java.util.List;

/**
* @author Zou
* @description 针对表【category(分类)】的数据库操作Service
* @createDate 2026-05-17 15:54:12
*/
public interface CategoryService extends IService<Category> {

    /**
     * 获取分类列表（带缓存）
     */
    List<Category> listCategoryCache();

    /**
     * 分页查询分类列表（带缓存）
     */
    Page<Category> queryCategoryPageCache(CategoryQueryDTO categoryQueryDTO);

    /**
     * 根据 id 获取分类（带缓存）
     */
    Category getCategoryByIdCache(Long id);

    /**
     * 清除分类相关缓存
     */
    void clearCategoryCache();
}

package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.entity.Category;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.category.CategoryAddDTO;
import org.example.pojo.dto.category.CategoryQueryDTO;
import org.example.pojo.dto.category.CategoryUpdateDTO;
import org.example.pojo.entity.Category;
import org.example.server.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类管理
 *
 * @author Zou
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 添加分类
     */
    @PostMapping("/add")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Long> addCategory(@RequestBody CategoryAddDTO categoryAddDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(categoryAddDTO) || StrUtil.isBlank(categoryAddDTO.getName()),
                ErrorCode.PARAMS_ERROR, "分类名称不能为空");

        Category category = new Category();
        category.setName(categoryAddDTO.getName());
        category.setCount(0);

        boolean result = categoryService.save(category);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(category.getId());
    }

    /**
     * 更新分类
     */
    @PostMapping("/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateCategory(@RequestBody CategoryUpdateDTO categoryUpdateDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(categoryUpdateDTO) || ObjUtil.isEmpty(categoryUpdateDTO.getId()),
                ErrorCode.PARAMS_ERROR);

        Category category = new Category();
        category.setId(categoryUpdateDTO.getId());
        category.setName(categoryUpdateDTO.getName());

        boolean result = categoryService.updateById(category);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/delete")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> deleteCategory(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest) || ObjUtil.isEmpty(deleteRequest.getId()),
                ErrorCode.PARAMS_ERROR);

        boolean result = categoryService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 获取全部分类列表
     */
    @GetMapping("/list")
    public BaseResponse<List<Category>> listCategory() {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(true,false,"count");
        List<Category> list = categoryService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 根据 id 获取分类
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Category> getCategoryById(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Category category = categoryService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(category), ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(category);
    }

    /**
     * 分页查询分类列表
     */
    @PostMapping("/page/query")
    public BaseResponse<Page<Category>> queryCategoryPage(@RequestBody CategoryQueryDTO categoryQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(categoryQueryDTO), ErrorCode.PARAMS_ERROR);

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

        Page<Category> page = categoryService.page(new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(page);
    }
}

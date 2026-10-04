package org.example.picture.core.interfaces;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.picture.core.domain.model.Category;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.shared.contract.DeleteRequest;
import org.example.picture.core.interfaces.dto.CategoryAddDTO;
import org.example.picture.core.interfaces.dto.CategoryQueryDTO;
import org.example.picture.core.interfaces.dto.CategoryUpdateDTO;
import org.example.picture.core.application.CategoryService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
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

        categoryService.clearCategoryCache();

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

        // 缓存暂时禁用
//        categoryService.clearCategoryCache();

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

        // 缓存暂时禁用
//        categoryService.clearCategoryCache();

        return ResultUtils.success(true);
    }

    /**
     * 获取全部分类列表（带缓存）
     */
    @GetMapping("/list")
    public BaseResponse<List<Category>> listCategory() {
        List<Category> list = categoryService.listCategoryCache();
        return ResultUtils.success(list);
    }

    /**
     * 根据 id 获取分类
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Category> getCategoryById(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Category category = categoryService.getCategoryByIdCache(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(category), ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(category);
    }

    /**
     * 分页查询分类列表（带缓存）
     */
    @PostMapping("/page/query")
    public BaseResponse<Page<Category>> queryCategoryPage(@RequestBody CategoryQueryDTO categoryQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(categoryQueryDTO), ErrorCode.PARAMS_ERROR);

        Page<Category> page = categoryService.queryCategoryPageCache(categoryQueryDTO);
        return ResultUtils.success(page);
    }
}

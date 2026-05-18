package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.tag.TagAddDTO;
import org.example.pojo.dto.tag.TagQueryDTO;
import org.example.pojo.dto.tag.TagUpdateDTO;
import org.example.pojo.entity.Category;
import org.example.pojo.entity.Tag;
import org.example.server.service.TagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标签管理
 *
 * @author Zou
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    private TagService tagService;

    /**
     * 添加标签
     */
    @PostMapping("/add")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Long> addTag(@RequestBody TagAddDTO tagAddDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(tagAddDTO) || StrUtil.isBlank(tagAddDTO.getName()),
                ErrorCode.PARAMS_ERROR, "标签名称不能为空");

        Tag tag = new Tag();
        tag.setName(tagAddDTO.getName());
        tag.setCount(0);

        boolean result = tagService.save(tag);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(tag.getId());
    }

    /**
     * 更新标签
     */
    @PostMapping("/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateTag(@RequestBody TagUpdateDTO tagUpdateDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(tagUpdateDTO) || ObjUtil.isEmpty(tagUpdateDTO.getId()),
                ErrorCode.PARAMS_ERROR);

        Tag tag = new Tag();
        tag.setId(tagUpdateDTO.getId());
        tag.setName(tagUpdateDTO.getName());

        boolean result = tagService.updateById(tag);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/delete")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> deleteTag(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest) || ObjUtil.isEmpty(deleteRequest.getId()),
                ErrorCode.PARAMS_ERROR);

        boolean result = tagService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取标签
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Tag> getTagById(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Tag tag = tagService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(tag), ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(tag);
    }

    /**
     * 获取全部分类列表
     */
    @GetMapping("/list")
    public BaseResponse<List<Tag>> listTag() {
        QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(true,false,"count");
        List<Tag> list = tagService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 分页查询标签列表
     */
    @PostMapping("/page/query")
    public BaseResponse<Page<Tag>> queryTagPage(@RequestBody TagQueryDTO tagQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(tagQueryDTO), ErrorCode.PARAMS_ERROR);

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

        Page<Tag> page = tagService.page(new Page<>(current, pageSize), queryWrapper);

        return ResultUtils.success(page);
    }
}

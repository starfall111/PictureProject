package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
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
import org.example.pojo.entity.Tag;
import org.example.server.service.TagService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
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

        tagService.clearTagCache();

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

        // 缓存暂时禁用
//        tagService.clearTagCache();

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

        // 缓存暂时禁用
//        tagService.clearTagCache();

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取标签
     */
    @GetMapping("/get/{id}")
    public BaseResponse<Tag> getTagById(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Tag tag = tagService.getTagByIdCache(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(tag), ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(tag);
    }

    /**
     * 获取全部标签列表（带缓存）
     */
    @GetMapping("/list")
    public BaseResponse<List<Tag>> listTag() {
        List<Tag> list = tagService.listTagCache();
        return ResultUtils.success(list);
    }

    /**
     * 分页查询标签列表（带缓存）
     */
    @PostMapping("/page/query")
    public BaseResponse<Page<Tag>> queryTagPage(@RequestBody TagQueryDTO tagQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(tagQueryDTO), ErrorCode.PARAMS_ERROR);

        Page<Tag> page = tagService.queryTagPageCache(tagQueryDTO);
        return ResultUtils.success(page);
    }
}

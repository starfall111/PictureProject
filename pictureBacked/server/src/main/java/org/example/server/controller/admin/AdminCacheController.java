package org.example.server.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.server.service.CategoryService;
import org.example.server.service.PictureService;
import org.example.server.service.TagService;
import org.example.server.service.impl.CachedPictureServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 管理员 — 缓存管理 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/cache")
public class AdminCacheController {

    @Resource(name = "cachedPictureService")
    private PictureService cachedPictureService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private TagService tagService;

    /**
     * 清理图片查询缓存
     */
    @PostMapping("/picture/clear")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> clearPictureCache() {
        if (cachedPictureService instanceof CachedPictureServiceImpl cached) {
            cached.invalidateAllQueryCache();
        }
        log.info("管理员清理图片查询缓存");
        return ResultUtils.success(true);
    }

    /**
     * 清理全站缓存（图片 + 分类 + 标签）
     */
    @PostMapping("/clearAll")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> clearAllCache() {
        // 清理图片查询缓存
        if (cachedPictureService instanceof CachedPictureServiceImpl cached) {
            cached.invalidateAllQueryCache();
        }

        // 清理分类缓存
        categoryService.clearCategoryCache();

        // 清理标签缓存
        tagService.clearTagCache();

        log.info("管理员清理全站缓存");
        return ResultUtils.success(true);
    }
}

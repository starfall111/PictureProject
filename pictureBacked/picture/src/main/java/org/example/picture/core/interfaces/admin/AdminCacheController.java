package org.example.picture.core.interfaces.admin;

import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.core.application.CategoryService;
import org.example.picture.core.application.PictureService;
import org.example.picture.core.application.TagService;
import org.example.picture.core.application.impl.CachedPictureServiceImpl;
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

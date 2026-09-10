package org.example.picture.social.interfaces;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.example.shared.annotation.CheckAuth;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.api.UserConstant;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.social.interfaces.dto.RecommendQueryDTO;
import org.example.identity.api.model.User;
import org.example.picture.social.interfaces.vo.RecommendVO;
import org.example.picture.social.application.RecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 推荐 API 控制器
 *
 * @author Zou
 */
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    /**
     * 推荐查询
     * homepage 场景无需登录，guess 场景需要登录（未登录降级为 homepage）
     */
    @PostMapping("/query")
    @RateLimit(resource = "recommend.query", dimensions = {RateLimitDimension.USER})
    public BaseResponse<RecommendVO> recommend(@RequestBody RecommendQueryDTO queryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(queryDTO), ErrorCode.PARAMS_ERROR);

        // 场景白名单校验
        String scene = queryDTO.getScene();
        if (StrUtil.isNotBlank(scene)) {
            ThrowUtils.throwIf(
                    !"homepage".equals(scene) && !"guess".equals(scene) && !"similar".equals(scene),
                    ErrorCode.PARAMS_ERROR, "不支持的推荐场景: " + scene);
        }

        // current 校验
        ThrowUtils.throwIf(queryDTO.getCurrent() < 1, ErrorCode.PARAMS_ERROR, "current 必须大于 0");

        User user = UserContext.get();
        Long userId = user != null ? user.getId() : null;

        RecommendVO result = recommendService.recommend(queryDTO, userId);
        return ResultUtils.success(result);
    }

    /**
     * 管理员手动触发热度全量重算
     */
    @PostMapping("/admin/rebuild")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Integer> adminRebuild() {
        int count = recommendService.rebuildHotScores();
        return ResultUtils.success(count);
    }
}

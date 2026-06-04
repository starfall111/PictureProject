package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.recommend.RecommendQueryDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.RecommendVO;
import org.example.server.service.RecommendService;
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

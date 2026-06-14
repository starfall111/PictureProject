package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.annotation.CheckAuth;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.common.constants.UserConstant;
import org.example.common.enums.SpaceLevelEnum;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.space.*;
import org.example.pojo.entity.Space;
import org.example.pojo.vo.SpaceVO;
import org.example.server.service.SpaceService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Zou
 */
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    //新增空间操作
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDTO spaceAddDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAddDTO), ErrorCode.PARAMS_ERROR);
        long id = spaceService.addSpace(spaceAddDTO);

        return ResultUtils.success(id);
    }

    //1.空间编辑信息：更改空间信息（管理员/普通用户）
    @PostMapping("/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUpdateDTO),ErrorCode.PARAMS_ERROR);

        boolean result = spaceService.updateSpace(spaceUpdateDTO);

        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(result);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDTO spaceEditDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceEditDTO),ErrorCode.PARAMS_ERROR);

        boolean result = spaceService.editSpace(spaceEditDTO);

        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(result);
    }

    //2.删除空间
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest) throws Exception{
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest),ErrorCode.PARAMS_ERROR);

        long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        boolean result= spaceService.deleteSpace(id);

        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(result);
    }

    //3.分页查询空间（管理员）
    @PostMapping("/query")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<Space>> querySpaceListAdmin(@RequestBody SpaceQueryDTO queryDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(queryDTO),ErrorCode.PARAMS_ERROR);

        Page<Space> spacePage = spaceService.querySpaceListAdmin(queryDTO);

        return ResultUtils.success(spacePage);
     }

    //4.根据id获取空间信息（普通用户）
    @GetMapping("/userSpace/{id}")
    public BaseResponse<List<SpaceVO>> getSpaceByUserId(@PathVariable long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        List<SpaceVO> spaceByUserId = spaceService.getSpaceByUserId(id);

        return ResultUtils.success(spaceByUserId);

    }

    //5.根据Id返回空间详细信息
    @GetMapping("/{id}")
    @RateLimit(resource = "space.get", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Space> getSpaceById(@PathVariable long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        Space spaceById = spaceService.getSpaceById(id);

        return ResultUtils.success(spaceById);
    }


    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        // 获取所有枚举
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}

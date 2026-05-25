package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.common.context.UserContext;
import org.example.common.enums.SpaceLevelEnum;
import org.example.common.enums.UserEnum;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.space.SpaceQueryDTO;
import org.example.pojo.dto.space.SpaceAddDTO;
import org.example.pojo.dto.space.SpaceEditDTO;
import org.example.pojo.dto.space.SpaceUpdateDTO;
import org.example.pojo.entity.*;
import org.example.pojo.entity.Space;
import org.example.pojo.vo.SpaceVO;
import org.example.server.mapper.UserMapper;
import org.example.server.service.SpaceService;
import org.example.server.mapper.SpaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Zou
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-05-21 20:57:25
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {
    @Resource
    private SpaceMapper spaceMapper;

    //本地锁优化
    private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

    //程序化事务，动态调控事务的结束
    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserMapper userMapper;


    @Override
    public long addSpace(SpaceAddDTO spaceAddDTO) {
        //校验空间信息
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddDTO, space);
        //拿到空间名称和权限信息
        String spaceName = space.getSpaceName();
        SpaceLevelEnum spaceLevel = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        //判断空间信息

        if (StrUtil.isBlank(spaceName)) {
            space.setSpaceName("默认空间");
        }

        validSpace(space);

        //权限判断，普通用户只能创建
        User user = UserContext.get();
        Long userId = user.getId();

        if (!SpaceLevelEnum.COMMON.equals(spaceLevel) && !UserEnum.ADMIN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
        }
        //构建入库信息

        space.setUserId(userId);
        space.setMaxCount(spaceLevel.getMaxCount());
        space.setMaxSize(spaceLevel.getMaxSize());

        //获取锁
        //本地锁，字符串常量池实现,存在缺陷，字符串常量池中的内容是不会自动释放的
        //String lock = String.valueOf(user.getId()).intern();
        //选用更优的本地锁
        Object lock = lockMap.computeIfAbsent(user.getId(), key -> new Object());
        try{
            synchronized (lock) {
                Long spaceId = transactionTemplate.execute(status -> {
                    //查重
                    boolean exists = lambdaQuery().eq(Space::getUserId, userId)
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有一个私有空间");

                    //写入数据库
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

                    return space.getId();
                });

                return Optional.ofNullable(spaceId).orElse(-1L);
            }
        }finally {
            lockMap.remove(user.getId());
        }
    }

    @Override
    public boolean updateSpace(SpaceUpdateDTO spaceUpdateDTO) {
        //转化实体类
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateDTO, space);

        validSpace(space);

        //判断空间是否存在
        Space oldSpace = this.getById(spaceUpdateDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.PARAMS_ERROR, "空间不存在");
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceLevelEnum),ErrorCode.PARAMS_ERROR);

        space.setMaxSize(spaceLevelEnum.getMaxSize());
        space.setMaxCount(spaceLevelEnum.getMaxCount());
        if (ObjUtil.isNotEmpty(spaceUpdateDTO.getMaxCount())) {
            space.setMaxCount(spaceUpdateDTO.getMaxCount());
        }
        if (ObjUtil.isNotEmpty(spaceUpdateDTO.getMaxSize())) {
            space.setMaxSize(spaceUpdateDTO.getMaxSize());
        }

        //更新
        return this.updateById(space);
    }

    /**
     * 用户修改空间信息
     *
     * @param spaceEditDTO
     * @return
     */
    @Override
    public boolean editSpace(SpaceEditDTO spaceEditDTO) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditDTO, space);

        //校验空间是否存在
        Space oldSpace = this.getById(space.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.PARAMS_ERROR, "该空间不存在");

        space.setSpaceLevel(oldSpace.getSpaceLevel());
        //校验空间信息
        validSpace(space);
        //校验权限
        validSpaceAuth(oldSpace, UserContext.get());

        return this.updateById(space);
    }

    @Override
    public boolean deleteSpace(long id) throws Exception {
        // 判断空间是否存在
        Space space = this.getById(id);

        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.PARAMS_ERROR, "该空间不存在");

        validSpaceAuth(space, UserContext.get());

        return this.removeById(id);
    }

    @Override
    public Page<Space> querySpaceListAdmin(SpaceQueryDTO queryDTO) {
        //构建查询条件
        QueryWrapper<Space> queryWrapper = getQueryWrapper(queryDTO);
        //Page封装

        return this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize()), queryWrapper);
    }

    @Override
    public List<SpaceVO> getSpaceByUserId(long id) {
        User user = UserContext.get();

        ThrowUtils.throwIf(!Long.valueOf(id).equals(user.getId()), ErrorCode.NO_AUTH_ERROR);

        List<Space> spaceList = lambdaQuery().eq(Space::getUserId, id)
                .list();

        List<SpaceVO> spaceVOList = spaceList.stream().map( space -> {
            SpaceVO spaceVO = new SpaceVO();
            BeanUtil.copyProperties(space,spaceVO);
            return spaceVO;
        }).toList();

        return spaceVOList;
    }

    @Override
    public Space getSpaceById(long id) {
        Space space = this.getById(id);

        ThrowUtils.throwIf(ObjUtil.isEmpty(space),ErrorCode.PARAMS_ERROR,"当前空间不存在");

        return space;
    }


    private void validSpace(Space space) {
        String spaceName = space.getSpaceName();

        SpaceLevelEnum spaceLevel = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

        ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
        ThrowUtils.throwIf(spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字");

        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceLevel), ErrorCode.PARAMS_ERROR, "空间级别不存在");
    }

    private void validSpaceAuth(Space space, User user) {
        //基础属性
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
        //空间级别信息判定
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "空间参数异常");
        //权限校验
        //是本人或者是管理员放行
        if (!user.getId().equals(space.getUserId()) && !UserEnum.ADMIN.equals(userEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO) {

        Long id = spaceQueryDTO.getId();
        String spaceName = spaceQueryDTO.getSpaceName();
        Integer spaceLevel = spaceQueryDTO.getSpaceLevel();
        Long userId = spaceQueryDTO.getUserId();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();


        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.orderBy(!ObjUtil.isEmpty(sortField), "ascend".equals(sortOrder), sortField);

        return queryWrapper;
    }

    //更严格的空间权限校验，只有本人才可以操作
    @Override
    public void validAuthUser(Space space, User user){
        if (!user.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}





package org.example.bootstrap.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.example.identity.api.model.User;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.notification.api.port.UserLookupPort;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户查询适配器 — 实现通知上下文的端口（装配层适配）
 * <p>
 * 逻辑平移自原 SystemMessageServiceImpl#resolveTargetUsers 的用户查询部分。
 *
 * @author Zou
 */
@Component
public class UserLookupAdapter implements UserLookupPort {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<Long> listAllUserIds() {
        return userMapper.selectList(new QueryWrapper<User>().select("id"))
                .stream().map(User::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listUserIdsByFilter(String filterRole, Integer filterSpaceLevel,
                                          Date filterRegisterStart, Date filterRegisterEnd) {
        return userMapper.selectIdsByFilter(
                filterRole,
                filterSpaceLevel,
                filterRegisterStart,
                filterRegisterEnd
        );
    }
}

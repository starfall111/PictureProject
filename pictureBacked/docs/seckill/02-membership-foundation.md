# Phase 2 — 会员基础能力

> 优先级：P0 | 依赖：Phase 1 | 预估工时：1.5d | 可与 Phase 3 并行

## 目标

在现有代码基础上改造会员体系：User 实体新增 VIP 字段、扩展权限校验注解、改造空间权限逻辑。此阶段完成后，系统已具备 VIP 身份标识和权益拦截能力。

---

## 1. 枚举类新增

### 1.1 VipTypeEnum

创建 `common/src/main/java/org/example/common/enums/VipTypeEnum.java`：

```java
package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * VIP 会员类型枚举
 */
@Getter
public enum VipTypeEnum {

    NORMAL("普通用户", 0),
    VIP("VIP会员", 1);

    private final String text;
    private final int value;

    VipTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static VipTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (VipTypeEnum e : VipTypeEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
```

### 1.2 CouponStatusEnum

创建 `common/src/main/java/org/example/common/enums/CouponStatusEnum.java`：

```java
package org.example.common.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 编码券状态枚举
 */
@Getter
public enum CouponStatusEnum {

    UNSOLD("未发放", 0),
    CLAIMED("已领取未使用", 1),
    ACTIVATED("已激活使用中", 2),
    EXPIRED("已过期", 3)

    private final String text;
    private final int value;

    CouponStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static CouponStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CouponStatusEnum e : CouponStatusEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
```

### 1.3 NotificationTypeEnum 扩展

在 `common/src/main/java/org/example/common/enums/NotificationTypeEnum.java` 中追加：

```java
VIP_ACTIVATED("VIP激活成功"),
VIP_EXPIRED("VIP已到期"),
VIP_EXPIRING("VIP即将到期"),
SECKILL_SUCCESS("秒杀成功"),
SECKILL_FAIL("秒杀失败"),
```

---

## 2. VIP 工具方法

创建 `common/src/main/java/org/example/common/util/VipUtil.java`：

```java
package org.example.common.util;

import org.example.pojo.entity.User;

import java.util.Date;

/**
 * VIP 状态判断工具
 */
public class VipUtil {

    private VipUtil() {
    }

    /**
     * 判断用户是否为有效 VIP 会员
     * 必须同时满足: vipType=1 AND vipExpireTime != null AND vipExpireTime > NOW()
     */
    public static boolean isActiveVip(User user) {
        return user != null
                && user.getVipType() != null
                && user.getVipType() == 1
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().after(new Date());
    }

    /**
     * 计算激活后的到期时间
     *
     * @param currentExpireTime 当前到期时间（可能为 null）
     * @param couponTypeDays    券类型天数（3/7/30）
     * @return 新的到期时间
     */
    public static Date calculateNewExpireTime(Date currentExpireTime, int couponTypeDays) {
        Date now = new Date();
        Date baseTime;

        if (currentExpireTime != null && currentExpireTime.after(now)) {
            // 当前会员未过期，在现有到期时间上累加
            baseTime = currentExpireTime;
        } else {
            // 当前非会员或已过期，从现在开始计算
            baseTime = now;
        }

        return new Date(baseTime.getTime() + (long) couponTypeDays * 24 * 3600 * 1000);
    }
}
```

---

## 3. User 实体 + VO 层改造

### 3.1 User 实体新增字段

在 `pojo/src/main/java/org/example/pojo/entity/User.java` 的 `userRole` 字段后追加：

```java
/**
 * VIP会员类型: 0-普通用户, 1-VIP会员
 */
private Integer vipType;

/**
 * VIP到期时间
 */
@TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
private Date vipExpireTime;

/**
 * 最近一次激活VIP时间
 */
@TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
private Date vipActivatedAt;

/**
 * VIP累计总天数
 */
private Integer vipTotalDays;
```

需要追加 import：
```java
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
```

### 3.2 LoginUserVO 新增字段

在 `pojo/src/main/java/org/example/pojo/vo/LoginUserVO.java` 中追加：

```java
/**
 * VIP会员类型: 0-普通用户, 1-VIP会员
 */
private Integer vipType;

/**
 * VIP到期时间
 */
private Date vipExpireTime;
```

### 3.3 UserProfileVO 新增字段

在 `pojo/src/main/java/org/example/pojo/vo/UserProfileVO.java` 中追加：

```java
/**
 * VIP会员类型
 */
private Integer vipType;

/**
 * VIP到期时间
 */
private Date vipExpireTime;

/**
 * 是否为有效VIP会员（计算字段，由 Service 层填充）
 */
private Boolean isActiveVip;
```

### 3.4 UserVO 新增字段（如存在）

检查 `pojo/src/main/java/org/example/pojo/vo/UserVO.java`，如果暴露了用户角色信息，也追加 `vipType` 字段。

### 3.5 UserMapper.xml 更新

修改 `server/src/main/resources/mapper/UserMapper.xml`，在 `Base_Column_List` 中追加 VIP 字段：

```xml
<sql id="Base_Column_List">
    id,userAccount,userPhone,userEmail,userPassword,userName,
    userAvatar,userProfile,userRole,
    banStatus,banEndTime,violationCount,lastViolationTime,
    vipType,vipExpireTime,vipActivatedAt,vipTotalDays,
    editTime,createTime,updateTime,isDelete
</sql>
```

**注意**：需要确认 `banStatus`、`banEndTime`、`violationCount`、`lastViolationTime` 这些字段是否已在 Base_Column_List 中。如果 User 表已有这些字段但 XML 中未列出，需要一并补齐。

---

## 4. CheckAuth 注解 + AOP 扩展

### 4.1 CheckAuth 注解

修改 `common/src/main/java/org/example/common/annotation/CheckAuth.java`：

```java
package org.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuth {

    /**
     * 必须具备的角色
     */
    String mustRole() default "";

    /**
     * 是否需要 VIP 会员权限
     */
    boolean requireVip() default false;
}
```

### 4.2 CheckAuthAop 扩展

修改 `server/src/main/java/org/example/server/aop/CheckAuthAop.java`，在角色校验通过后追加 VIP 校验：

```java
package org.example.server.aop;

import org.example.pojo.entity.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.enums.UserEnum;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.VipUtil;
import org.example.server.service.UserService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * @author Zou
 */
@Aspect
@Component
public class CheckAuthAop {

    @Resource(name = "dbUserService")
    private UserService userService;

    @Around("@annotation(checkAuth)")
    public Object checkRoleInterceptor(ProceedingJoinPoint checkPoint, CheckAuth checkAuth) throws Throwable {
        // 获取当前接口需要的权限
        String mustRole = checkAuth.mustRole();
        UserEnum mustRoleEnum = UserEnum.getByValue(mustRole);
        // 获取当前登录用户
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        user = userService.getById(user.getId());
        // 判断角色权限是否满足
        UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
        ThrowUtils.throwIf(UserEnum.ADMIN.equals(mustRoleEnum) && !UserEnum.ADMIN.equals(userEnum), ErrorCode.NO_AUTH_ERROR);

        // VIP 会员权限校验
        if (checkAuth.requireVip()) {
            ThrowUtils.throwIf(!VipUtil.isActiveVip(user), ErrorCode.NO_AUTH_ERROR, "该功能需要VIP会员");
        }

        // 放行
        return checkPoint.proceed();
    }
}
```

---

## 5. SpaceServiceImpl 权限逻辑改造

修改 `server/src/main/java/org/example/server/service/impl/SpaceServiceImpl.java` 中的 `addSpace` 方法。

### 5.1 定位当前逻辑

找到类似以下代码段：

```java
if (!SpaceLevelEnum.COMMON.equals(spaceLevel) && !UserEnum.ADMIN.getValue().equals(user.getUserRole())) {
    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
}
```

### 5.2 替换为

```java
boolean isAdmin = UserEnum.ADMIN.getValue().equals(user.getUserRole());
boolean isVip = VipUtil.isActiveVip(user);

// 非普通版空间：管理员 或 有效VIP会员可创建
if (!SpaceLevelEnum.COMMON.equals(spaceLevel)) {
    if (!isAdmin && !isVip) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "升级为VIP会员以解锁专业版空间");
    }
}
// 旗舰版空间仅管理员可创建
if (SpaceLevelEnum.FLAGSHIP.equals(spaceLevel) && !isAdmin) {
    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "旗舰版空间仅管理员可创建");
}
```

### 5.3 需要新增 import

```java
import org.example.common.util.VipUtil;
import org.example.common.enums.SpaceLevelEnum;
```

---

## 6. UserServiceImpl VO 转换更新

在 `server/src/main/java/org/example/server/service/impl/UserServiceImpl.java` 中，找到所有 User → LoginUserVO / UserVO / UserProfileVO 的转换方法，确保新增的 VIP 字段被正确拷贝。

具体需要检查的方法：
- `userToLoginUserVO(User user)` — 追加 `vipType`、`vipExpireTime`
- `userToUserProfileVO(User user)` — 追加 `vipType`、`vipExpireTime`、`isActiveVip`
- 其他 User → VO 转换方法

UserProfileVO 的 `isActiveVip` 字段需要在转换时计算：

```java
userProfileVO.setIsActiveVip(VipUtil.isActiveVip(user));
```

---

## 验证清单

- [ ] `VipTypeEnum` / `CouponStatusEnum` 编译通过
- [ ] `NotificationTypeEnum` 新增枚举值无冲突
- [ ] `User` 实体新增 4 个 VIP 字段，MyBatis-Plus 能正确映射
- [ ] `LoginUserVO` 序列化 JSON 包含 vipType/vipExpireTime
- [ ] `UserProfileVO` 的 isActiveVip 能正确计算
- [ ] `UserMapper.xml` 的 Base_Column_List 包含所有新字段
- [ ] `CheckAuth(requireVip = true)` 注解标注的接口，非 VIP 用户访问返回 403
- [ ] VIP 用户访问 `@CheckAuth(requireVip = true)` 接口正常放行
- [ ] 普通用户创建专业版空间被拒绝，VIP 用户可创建
- [ ] 管理员创建旗舰版空间不受影响
- [ ] `VipUtil.isActiveVip()` 对 null/过期/有效的各种情况返回正确结果

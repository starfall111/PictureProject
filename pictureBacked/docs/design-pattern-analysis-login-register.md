# 设计模式分析：模板方法 vs 策略模式

> 基于 PictureProject 登录注册模块的重构讨论，记录设计决策过程与思考方法。

---

## 一、问题背景

### 当前代码现状

`UserServiceImpl` 中登录和注册通过 `switch(type)` 处理三种账号类型（账号/手机/邮箱），存在大量重复流程：

**注册 (`userRegister`)**：
```
case 0 (账号): 校验密码 → 检查账号重复 → 加密密码 → 设置 account/password
case 1 (手机): 校验格式 → 检查手机重复 → 验证码验证 → 生成随机账号 → 设置 phone/account/password
case 2 (邮箱): 校验格式 → 检查邮箱重复 → 验证码验证 → 生成随机账号 → 设置 email/account/password
```

**登录 (`userLogin`)**：
- 密码登录：三个 case 几乎完全一样，只有查询字段不同
- 验证码登录：case 1 和 case 2 完全相同，只是字段不同

### 核心问题

重复的不是单行代码，而是**流程结构**。每种账号类型都在重复同一个算法骨架，只是步骤细节不同。

---

## 二、两种模式的本质区别

### 判断标准

| 特征 | 模板方法 | 策略模式 |
|------|---------|---------|
| 变的是什么 | **步骤的细节** | **整体行为（算法本身）** |
| 不变的是什么 | **流程骨架（步骤顺序）** | 调用接口 |
| 各实现之间 | 有共同骨架，步骤可复用 | 完全独立，互相替换 |
| 关系 | 父子类继承 | 接口 + 组合 |

### 一句话判断

> **如果删掉 switch 后，各 case 之间还能看到相同的步骤顺序，用模板方法。**
> **如果各 case 做的是完全不同的事情，只是语义上都叫"发送"，用策略模式。**

---

## 三、登录注册场景：模板方法

### 3.1 为什么选模板方法

以注册为例，三种账号类型的流程骨架是**固定的**：

```
校验格式 → 检查重复 → 验证凭证 → 构建用户 → 入库
```

每一步的具体实现因账号类型而异，但**步骤顺序和整体结构不变**。这是模板方法的经典场景。

如果用策略模式，每个策略类都要独立实现完整流程，手机和邮箱的策略代码几乎一模一样，重复从 switch 转移到了类里。

### 3.2 为什么是两个模板方法，不是一个

登录和注册的流程骨架**看起来相似但语义相反**：

| 步骤 | 注册 | 登录 |
|------|------|------|
| 数据库查询 | 用户**不能存在** | 用户**必须存在** |
| 验证凭证 | 密码一致性 / 验证码 | 密码比对 / 验证码 |
| 核心动作 | **创建**新记录 | **读取**已有记录 |
| 副作用 | 写入数据库 | 写入 Session |
| 返回值 | `long`（ID） | `LoginUserVO` |

如果硬塞进一个模板方法，就需要 `isLogin` 标志位做条件分支 — 这恰恰是模板方法要消除的东西。

**职责由调用方（Controller）决定**：

```
POST /user/register → registerHandler.execute(dto)     → 走注册模板
POST /user/login    → loginHandler.execute(dto, req)    → 走登录模板
```

### 3.3 两个维度的变异如何处理

登录场景存在**两个独立的变异维度**：

- **维度一**：账号类型（账号 / 手机 / 邮箱）→ 格式校验、查询字段不同
- **维度二**：登录方式（密码 / 验证码）→ 验证凭证逻辑不同

关键观察：**登录方式的差异对所有账号类型都一样**。

不管手机还是邮箱，密码登录都是"用户必须存在 + 比对密码"；验证码登录都是"用户可以不存在 + 自动注册"。

因此：
- **账号类型** → 模板方法的抽象钩子（子类实现）
- **登录方式** → 模板方法骨架内的**固定分支**（所有子类通用）

```
                    账号类型维度（模板钩子）    登录方式维度（骨架内分支）
                           ↓                        ↓
validateFormat()      ← 子类各自实现            ← 不参与
findUser()            ← 子类提供字段名           ← 不参与
verifyCredentials()   ← 不参与                  ← 骨架内 if/else
recordSession()       ← 不参与                  ← 不参与（公共）
```

如果把 `verifyCredentials` 也做成抽象钩子，每个子类都要写密码+验证码两套逻辑，密码登录代码被复制三遍，验证码登录代码也被复制三遍 — 问题只是从 switch 变成了子类间的重复。

### 3.4 最终结构

```
// ==================== 注册模板 ====================
AbstractRegisterHandler
├── execute(dto)                          // 模板方法
│   ├── validateFormat(account)           // 子类实现：账号/手机/邮箱格式校验
│   ├── checkDuplicate(account)           // 公共实现：参数化字段名
│   ├── verifyCredentials(dto)            // 子类实现：密码 or 验证码
│   └── buildAndSave(dto)                 // 子类实现：构建 User 实体
│
├── AccountRegisterHandler
├── PhoneRegisterHandler
└── EmailRegisterHandler

// ==================== 登录模板 ====================
AbstractLoginHandler
├── execute(dto, request)                 // 模板方法
│   ├── validateFormat(account)           // 子类实现（抽象钩子）
│   ├── findUser(account)                 // 公共实现，子类提供字段名（抽象钩子）
│   ├── if (密码登录) verifyByPassword()  // 骨架内分支，所有子类共享
│   ├── if (验证码登录) verifyByCode()    // 骨架内分支，所有子类共享
│   └── recordSession(user, request)      // 公共实现
│
├── AccountLoginHandler  → validateFormat + getQueryField
├── PhoneLoginHandler    → validateFormat + getQueryField
└── EmailLoginHandler    → validateFormat + getQueryField
```

### 3.5 骨架代码示例

```java
public abstract class AbstractLoginHandler {

    // ========== 账号类型钩子（子类实现） ==========
    protected abstract void validateFormat(String account);
    protected abstract String getQueryField();

    // ========== 模板方法（final 防止子类覆盖） ==========
    public final LoginUserVO execute(UserLoginDTO dto, HttpServletRequest request) {
        String account = dto.getAccount();

        // 1. 校验格式 — 子类决定怎么校验
        validateFormat(account);

        // 2. 查找用户 — 公共实现，子类提供字段名
        User user = findUser(getQueryField(), account);

        // 3. 验证凭证 — 登录方式分支，对所有账号类型通用
        if (dto.getIsVerityCode() == 0) {
            user = verifyByPassword(dto, user);
        } else {
            user = verifyByCode(dto, user);
        }

        // 4. 记录 Session — 公共实现
        return recordSession(user, request);
    }

    // ========== 登录方式：密码 ==========
    private User verifyByPassword(UserLoginDTO dto, User user) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), "用户不存在");
        String encrypted = getEncryptPassword(dto.getPassword());
        ThrowUtils.throwIf(!encrypted.equals(user.getUserPassword()), "账号或密码错误");
        return user;
    }

    // ========== 登录方式：验证码 ==========
    private User verifyByCode(UserLoginDTO dto, User user) {
        if (ObjUtil.isEmpty(user)) {
            // 不存在则自动注册，委托给注册模板
            long id = registerHandler.execute(
                new UserRegisterDTO(dto.getType(), dto.getAccount(),
                                    null, null, dto.getVerityCode()));
            user = userMapper.selectById(id);
        } else {
            verityCodeService.verityCode(dto.getAccount(), dto.getVerityCode());
        }
        return user;
    }

    // ========== 子类示例 ==========
    // AccountLoginHandler:
    //   validateFormat → account.length() < 4
    //   getQueryField  → USER_ACCOUNT_FAILED

    // PhoneLoginHandler:
    //   validateFormat → 正则 ^1[3-9]\d{9}$
    //   getQueryField  → USER_PHONE_FAILED

    // EmailLoginHandler:
    //   validateFormat → 邮箱正则
    //   getQueryField  → USER_EMAIL_FAILED
}
```

---

## 四、验证码/通知场景：策略模式

### 4.1 为什么选策略模式

验证码发送和审批通知的特点：

- 各渠道（SMS、Email、微信推送）的实现**完全独立**
- 没有共享的流程骨架，只有"发送"这个**语义上的共性**
- 一次操作可能需要**同时触发多个渠道**
- 新增渠道只需加一个策略类，不影响已有实现

```java
// 当前代码 — 验证码发送
switch(type){
    case 1 -> { aliSMSUtil.SMSSendCode(account, code, "100001", 5); }
    case 2 -> { emailUtil.sendVerificationCode(account, code); }
}

// 当前代码 — 审批通知（只有邮件）
emailUtil.sendReviewNotice(uploader.getUserEmail(), pictureName, passed, message);
// 用户没绑邮箱就收不到通知
```

### 4.2 策略模式结构

```
VerificationCodeSender (策略接口)
├── sendCode(String account, String code)
│
├── SmsCodeSender       implements VerificationCodeSender
├── EmailCodeSender     implements VerificationCodeSender
└── WeChatCodeSender    implements VerificationCodeSender  (未来扩展)

// 使用
VerificationCodeSender sender = senderFactory.getSender(type);
sender.sendCode(account, code);
```

```
NotificationSender (策略接口)
├── send(String to, String subject, String content)
│
├── EmailNotifier       implements NotificationSender
├── SmsNotifier         implements NotificationSender
├── WeChatNotifier      implements NotificationSender
└── WebSocketNotifier   implements NotificationSender

// 审批通知时组合多个策略
List<NotificationSender> senders = resolveUserChannels(uploader);
senders.forEach(s -> s.send(to, subject, content));
```

### 4.3 策略模式也可以和模板方法组合

验证码发送内部可以同时使用模板方法：

```
发送验证码的模板方法：
  生成验证码 → 存入 Redis → 调用策略发送

其中"调用策略发送"这一步委托给策略模式选择具体渠道。
```

---

## 五、可扩展性设计

### 5.1 防撞库机制（登录模板的钩子方法）

防撞库是一个**与账号类型无关**的横切关注点，通过钩子方法预留：

```java
// AbstractLoginHandler
public final LoginUserVO execute(UserLoginDTO dto, HttpServletRequest request) {
    validateFormat(dto.getAccount());
    preCheck(dto.getAccount());        // ← 钩子，默认空实现
    User user = findUser(getQueryField(), dto.getAccount());
    // ...
}

// 默认空实现，子类可以选择性覆盖
protected void preCheck(String account) {
    // 默认不做任何检查
}
```

**扩展路径**：

- **阶段一**：直接在父类覆盖钩子，所有登录类型自动获得防撞库能力
- **阶段二**：如果限流策略也需要灵活切换（固定窗口/滑动窗口/令牌桶），钩子内部再委托给策略模式

### 5.2 新增登录方式

当登录方式从两种扩展到更多（微信扫码、OAuth 等）时，骨架内的 `if/else` 分支变得复杂，可以将其升级为策略委托：

```java
// 从骨架内分支渐进演化为策略
CredentialVerifier verifier = verifierFactory.getVerifier(dto.getIsVerityCode());
user = verifier.verify(dto, user);
```

### 5.3 项目中已有的模板方法参考

`PictureUploadTemplate` 已成功使用模板方法：

```
PictureUploadTemplate
├── validateImage()           // 公共
├── uploadOriginImage()       // 子类实现（File/URL 差异点）
├── compressAndUpload()       // 公共
└── saveToDatabase()          // 公共
```

登录/注册的模板方法思路与此完全一致。

---

## 六、决策总结

| 场景 | 模式 | 原因 |
|------|------|------|
| 登录/注册（按账号类型） | 模板方法 | 流程骨架固定，步骤细节可变 |
| 密码/验证码（按登录方式） | 骨架内分支 | 对所有账号类型逻辑相同，暂不需要策略 |
| 验证码发送（按渠道） | 策略模式 | 各渠道实现完全独立，可替换 |
| 审批通知（按渠道） | 策略模式 | 多渠道并行，各渠道独立 |
| 防撞库/限流 | 钩子方法 | 横切关注点，所有类型共享 |
| 未来新增登录方式 | 分支 → 策略 | 渐进演化，当前不过度设计 |

### 设计原则

1. **对齐变化维度**：每个维度只在一个地方变化，不交叉
2. **最小抽象**：用最简单的结构解决当前问题，不为假设的未来过度设计
3. **渐进演化**：`if/else` → 钩子 → 策略，按实际需求逐步升级
4. **优先组合**：模板方法和策略模式可以组合使用，不必二选一

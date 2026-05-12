# 项目：PictureProject

## Git 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>
```

### Type 列表

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `refactor` | 重构 |
| `docs` | 文档变更 |
| `style` | 格式调整（不影响逻辑） |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖变更 |

### 规则

- subject 使用中文，不超过 50 个字符
- 用祈使句：用"添加"而不是"添加了"
- 一个 commit 做一件事，不相关的改动分开提交
- body 可选，用于解释 why（为什么需要这个改动）
- 不要提交 IDE 配置、target 构建产物、敏感信息

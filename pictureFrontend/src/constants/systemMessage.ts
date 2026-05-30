// 消息状态
export const SYSTEM_MESSAGE_STATUS_ENUM = {
  DRAFT: 0,
  PUBLISHED: 1,
  REVOKED: 2,
}

export const SYSTEM_MESSAGE_STATUS_MAP: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已撤回',
}

export const SYSTEM_MESSAGE_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已撤回', value: 2 },
]

// 标签颜色映射
export const SYSTEM_MESSAGE_STATUS_COLOR: Record<number, string> = {
  0: 'default',
  1: 'green',
  2: 'orange',
}

// 发送模式
export const SEND_MODE_MAP: Record<string, string> = {
  'IMMEDIATE': '立即发送',
  'SCHEDULED': '定时发送',
}

export const SEND_MODE_OPTIONS = [
  { label: '立即发送', value: 'IMMEDIATE' },
  { label: '定时发送', value: 'SCHEDULED' },
]

// 目标类型
export const TARGET_TYPE_MAP: Record<string, string> = {
  'ALL': '全部用户',
  'ROLE': '按角色',
  'SPACE_LEVEL': '按空间等级',
  'REGISTER_TIME': '按注册时间',
}

export const TARGET_TYPE_OPTIONS = [
  { label: '全部用户', value: 'ALL' },
  { label: '按角色', value: 'ROLE' },
  { label: '按空间等级', value: 'SPACE_LEVEL' },
  { label: '按注册时间', value: 'REGISTER_TIME' },
]

// 角色筛选选项（targetType=ROLE 时使用）
export const FILTER_ROLE_OPTIONS = [
  { label: '管理员', value: 'admin' },
  { label: '普通用户', value: 'user' },
]

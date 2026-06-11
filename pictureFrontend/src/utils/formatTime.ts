import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 相对时间格式化：7 天内显示"刚刚/X分钟前/X小时前"，超过 7 天显示 YYYY-MM-DD HH:mm
 */
export function formatTime(time?: string | null): string {
  if (!time) return '-'
  const d = dayjs(time)
  if (!d.isValid()) return '-'
  const now = dayjs()
  if (now.diff(d, 'day') >= 7) return d.format('YYYY-MM-DD HH:mm')
  return d.fromNow()
}

/**
 * 固定日期时间格式：YYYY-MM-DD HH:mm
 */
export function formatDateTime(time?: string | null): string {
  if (!time) return '-'
  const d = dayjs(time)
  if (!d.isValid()) return '-'
  return d.format('YYYY-MM-DD HH:mm')
}

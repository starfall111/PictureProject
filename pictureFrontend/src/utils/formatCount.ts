/**
 * 格式化数字计数
 * < 1000 → 原数字
 * ≥ 1k → 如 1.2k
 * ≥ 1w → 如 3.5w
 * ≥ 1b → 如 1.2b
 */
export function formatCount(value: number | undefined | null): string {
  if (value == null || value < 0) return '0'
  if (value < 1000) return String(value)

  if (value < 10000) {
    const k = value / 1000
    return `${Number.isInteger(k) ? k : k.toFixed(1)}k`
  }

  if (value < 100000000) {
    const w = value / 10000
    return `${Number.isInteger(w) ? w : w.toFixed(1)}w`
  }

  const b = value / 100000000
  return `${Number.isInteger(b) ? b : b.toFixed(1)}b`
}

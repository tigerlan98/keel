import { format, isValid, parseISO } from 'date-fns'

/**
 * 统一列表时间展示；后端返回不带时区的 ISO 字符串，按本地时间解析。
 */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }

  const date = parseISO(value)
  return isValid(date) ? format(date, 'yyyy-MM-dd HH:mm:ss') : value
}

import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

// 配置dayjs
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 解析后端返回的时间（支持数组格式和字符串格式）
 * @param {string|Array|Date} time - 时间
 * @returns {dayjs.Dayjs|null} dayjs对象
 */
function parseTime(time) {
  if (!time) return null

  // 处理数组格式: [2025, 12, 14, 10, 30, 0] (LocalDateTime序列化)
  if (Array.isArray(time)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = time
    return dayjs(new Date(year, month - 1, day, hour, minute, second))
  }

  // 处理字符串格式 (yyyy-MM-dd HH:mm:ss 或 ISO 8601)
  // 后端已配置 time-zone: Asia/Shanghai，返回的是东八区时间
  if (typeof time === 'string') {
    // dayjs 原生支持 ISO 8601 格式，直接解析即可
    // 仅替换 T 为空格以兼容旧格式，保留 Z 让 dayjs 正确处理 UTC
    return dayjs(time)
  }

  // 处理Date对象
  return dayjs(time)
}

/**
 * 格式化时间 - 完整格式
 * @param {string|Array|Date} time - 时间
 * @returns {string} 格式化后的时间 (YYYY-MM-DD HH:mm:ss)
 */
export function formatTime(time) {
  const parsed = parseTime(time)
  if (!parsed || !parsed.isValid()) return '-'
  return parsed.format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化时间 - 相对时间（用于列表显示）
 * @param {string|Array|Date} dateTime - 时间
 * @returns {string} 格式化后的时间 (今天 HH:mm / 昨天 HH:mm / MM-DD HH:mm)
 */
export function formatTimeRelative(dateTime) {
  const date = parseTime(dateTime)
  if (!date || !date.isValid()) return ''

  const now = dayjs()
  const today = now.startOf('day')
  const yesterday = today.subtract(1, 'day')

  if (date.isAfter(today)) {
    return '今天 ' + date.format('HH:mm')
  } else if (date.isAfter(yesterday)) {
    return '昨天 ' + date.format('HH:mm')
  } else {
    return date.format('MM-DD HH:mm')
  }
}

/**
 * 格式化时间 - 自然语言（多久以前）
 * @param {string|Array|Date} time - 时间
 * @returns {string} 格式化后的时间 (3分钟前 / 2小时前 / 2天前 / 完整日期)
 */
export function formatTimeAgo(time) {
  const parsed = parseTime(time)
  if (!parsed || !parsed.isValid()) return '-'

  const diffHours = dayjs().diff(parsed, 'hour')

  // 24小时内显示相对时间
  if (diffHours < 24) {
    return parsed.fromNow()
  }

  // 超过24小时显示完整时间
  return parsed.format('MM-DD HH:mm')
}

/**
 * 格式化日期 - 只显示日期部分
 * @param {string|Array|Date} time - 时间
 * @returns {string} 格式化后的日期 (YYYY-MM-DD)
 */
export function formatDate(time) {
  const parsed = parseTime(time)
  if (!parsed || !parsed.isValid()) return '-'
  return parsed.format('YYYY-MM-DD')
}

/**
 * 格式化时长(秒转为时分秒)
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时长
 */
export function formatDuration(seconds) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

/**
 * 计算时间差（分钟）
 * @param {string|Date} startTime - 开始时间
 * @param {string|Date} endTime - 结束时间
 * @returns {number} 时间差（分钟）
 */
export function diffInMinutes(startTime, endTime) {
  if (!startTime || !endTime) return 0
  return dayjs(endTime).diff(dayjs(startTime), 'minute')
}

/**
 * 计算时间差（小时）
 * @param {string|Date} startTime - 开始时间
 * @param {string|Date} endTime - 结束时间
 * @returns {number} 时间差（小时）
 */
export function diffInHours(startTime, endTime) {
  if (!startTime || !endTime) return 0
  return dayjs(endTime).diff(dayjs(startTime), 'hour')
}

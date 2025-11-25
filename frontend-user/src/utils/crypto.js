/**
 * 加密工具类
 * 
 * 功能:
 * 1. 密码SHA256哈希（前端加密传输）
 * 2. 敏感数据脱敏
 * 
 * 注意:
 * - 前端SHA256不能替代后端BCrypt
 * - 前端加密仅用于传输安全，防止明文密码泄露
 * - 必须配合HTTPS使用
 */

import CryptoJS from 'crypto-js'

/**
 * SHA256哈希加密
 * @param {string} text - 待加密文本
 * @returns {string} 加密后的哈希值
 */
export function sha256(text) {
  if (!text) {
    return ''
  }
  return CryptoJS.SHA256(text).toString()
}

/**
 * 手机号脱敏
 * @param {string} phone - 手机号
 * @returns {string} 脱敏后的手机号
 * @example
 * maskPhone('13812345678') => '138****5678'
 */
export function maskPhone(phone) {
  if (!phone || phone.length !== 11) {
    return phone
  }
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 身份证号脱敏
 * @param {string} idCard - 身份证号
 * @returns {string} 脱敏后的身份证号
 * @example
 * maskIdCard('110101199001011234') => '110101****1234'
 */
export function maskIdCard(idCard) {
  if (!idCard || idCard.length < 8) {
    return idCard
  }
  return idCard.replace(/^(.{6})(?:\d+)(.{4})$/, '$1****$2')
}

/**
 * 银行卡号脱敏
 * @param {string} bankCard - 银行卡号
 * @returns {string} 脱敏后的银行卡号
 * @example
 * maskBankCard('6222021234567890123') => '622202 **** 0123'
 */
export function maskBankCard(bankCard) {
  if (!bankCard || bankCard.length < 8) {
    return bankCard
  }
  return bankCard.replace(/^(.{6})(?:\d+)(.{4})$/, '$1 **** $2')
}

/**
 * 姓名脱敏
 * @param {string} name - 姓名
 * @returns {string} 脱敏后的姓名
 * @example
 * maskName('张三') => '张*'
 * maskName('欧阳修') => '欧阳*'
 */
export function maskName(name) {
  if (!name || name.length === 0) {
    return name
  }
  if (name.length === 1) {
    return name
  }
  if (name.length === 2) {
    return name[0] + '*'
  }
  // 保留姓氏（可能是复姓）
  return name.substring(0, name.length - 1) + '*'
}

/**
 * 密码强度检测
 * @param {string} password - 密码
 * @returns {Object} 强度信息
 * @example
 * checkPasswordStrength('Abc123!@') => { level: 'strong', score: 4, tips: [] }
 */
export function checkPasswordStrength(password) {
  if (!password) {
    return { level: 'weak', score: 0, tips: ['密码不能为空'] }
  }

  const result = {
    level: 'weak',
    score: 0,
    tips: []
  }

  // 长度检查
  if (password.length < 6) {
    result.tips.push('密码长度至少6位')
    return result
  }
  if (password.length >= 8) {
    result.score += 1
  }
  if (password.length >= 12) {
    result.score += 1
  }

  // 包含小写字母
  if (/[a-z]/.test(password)) {
    result.score += 1
  } else {
    result.tips.push('建议包含小写字母')
  }

  // 包含大写字母
  if (/[A-Z]/.test(password)) {
    result.score += 1
  } else {
    result.tips.push('建议包含大写字母')
  }

  // 包含数字
  if (/\d/.test(password)) {
    result.score += 1
  } else {
    result.tips.push('建议包含数字')
  }

  // 包含特殊字符
  if (/[!@#$%^&*()_+\-=\[\]{};':"\|,.<>\/?]/.test(password)) {
    result.score += 1
  } else {
    result.tips.push('建议包含特殊字符')
  }

  // 评级
  if (result.score <= 2) {
    result.level = 'weak'
  } else if (result.score <= 4) {
    result.level = 'medium'
  } else {
    result.level = 'strong'
  }

  return result
}

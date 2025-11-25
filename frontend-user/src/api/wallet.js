import request from '@/utils/request'

/**
 * 获取账户余额
 */
export function getBalance() {
  return request({
    url: '/users/balance',
    method: 'get'
  })
}

/**
 * 充值
 * @param {object} data 充值信息 {amount, paymentMethod}
 */
export function recharge(data) {
  return request({
    url: '/users/recharge',
    method: 'post',
    data
  })
}

/**
 * 获取交易记录
 * @param {object} params 查询参数 {page, size, type}
 */
export function getTransactions(params) {
  return request({
    url: '/users/transactions',
    method: 'get',
    params
  })
}

/**
 * 提现
 * @param {object} data 提现信息 {amount, bankAccount}
 */
export function withdraw(data) {
  return request({
    url: '/users/withdraw',
    method: 'post',
    data
  })
}

import request from '@/utils/request'

// 获取聊天历史记录
export function getChatHistory(params) {
  return request({
    url: '/chat/history',
    method: 'get',
    params
  })
}

// 获取在线用户列表
export function getOnlineUsers(params) {
  return request({
    url: '/chat/online-users',
    method: 'get',
    params
  })
}

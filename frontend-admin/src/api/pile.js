import request from '@/utils/request'

export function getAllPiles() {
  return request({ url: '/piles', method: 'get' })
}

export function getPilesByStation(stationId) {
  return request({ url: `/piles/station/${stationId}`, method: 'get' })
}

export function getFaultPiles() {
  return request({ url: '/piles/fault', method: 'get' })
}

export function updatePileStatus(id, status) {
  return request({ url: `/piles/${id}/status`, method: 'patch', params: { status } })
}

export function createPile(data) {
  return request({ url: '/piles', method: 'post', data })
}

export function updatePile(id, data) {
  return request({ url: `/piles/${id}`, method: 'put', data })
}

export function deletePile(id) {
  return request({ url: `/piles/${id}`, method: 'delete' })
}

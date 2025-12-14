import request from '../utils/request.js'

/**
 * 获取弹幕列表
 * @param {string} videoId 视频ID
 * @param {number} max 最大数量
 * @returns {Promise}
 */
export const getDanmakuList = (videoId, max = 3000) => {
  return request({
    url: '/danmakuv3',
    method: 'get',
    params: { id: videoId, max }
  })
}

/**
 * 发送弹幕
 * @param {Object} danmaku 弹幕对象 {id, author, time, text, color, type}
 * @returns {Promise}
 */
export const sendDanmaku = (danmaku) => {
  return request({
    url: '/danmakuv3',
    method: 'post',
    data: danmaku
  })
}

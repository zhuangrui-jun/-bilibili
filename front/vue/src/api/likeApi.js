import request from '../utils/request.js'

/**
 * 点赞/取消点赞
 * @param {number|string} videoId 视频ID
 * @returns {Promise}
 */
export const toggleLike = (videoId) => {
  return request({
    url: `/video/like/${videoId}`,
    method: 'post'
  })
}

/**
 * 获取点赞状态（需要登录）
 * @param {number|string} videoId 视频ID
 * @returns {Promise}
 */
export const getLikeStatus = (videoId) => {
  return request({
    url: `/video/like/${videoId}`,
    method: 'get'
  })
}

/**
 * 获取点赞数（不需要登录）
 * @param {number|string} videoId 视频ID
 * @returns {Promise}
 */
export const getLikeCount = (videoId) => {
  return request({
    url: `/video/like/count/${videoId}`,
    method: 'get'
  })
}



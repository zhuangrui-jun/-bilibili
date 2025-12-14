import request from '../utils/request.js'

/**
 * 获取所有视频列表
 * @returns {Promise}
 */
export const getAllVideos = () => {
  return request({
    url: '/video/list',
    method: 'get'
  })
}


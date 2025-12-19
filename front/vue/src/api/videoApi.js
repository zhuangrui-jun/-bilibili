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

/**
 * 根据ID获取视频详情
 * @param {string|number} videoId 视频ID
 * @returns {Promise}
 */
export const getVideoDetail = (videoId) => {
  return request({
    url: `/video/detail/${videoId}`,
    method: 'get'
  })
}

/**
 * 分页获取用户视频列表
 * @param {object} params { creatorId, page, pageSize }
 * @returns {Promise}
 */
export const getVideoPage = (params) => {
  return request({
    url: '/video/page',
    method: 'get',
    params
  })
}


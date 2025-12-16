import request from '../utils/request.js'

/**
 * 保存草稿
 * @param {Object} data 草稿数据 {title, videoUrl, coverUrl}
 * @returns {Promise}
 */
export const saveDraft = (data) => {
  return request({
    url: '/video/draft/save',
    method: 'post',
    data
  })
}

/**
 * 提交视频
 * @param {Object} data 视频数据 {draftId, title, videoUrl, coverUrl}
 * @returns {Promise}
 */
export const submitVideo = (data) => {
  return request({
    url: '/video/submit',
    method: 'post',
    data
  })
}

/**
 * 获取草稿列表（获取当前用户的草稿）
 * @returns {Promise}
 */
export const getDraftList = () => {
  return request({
    url: '/video/draft/list',
    method: 'get'
  })
}

/**
 * 获取草稿详情
 * @param {string|number} draftId 草稿ID
 * @returns {Promise}
 */
export const getDraftDetail = (draftId) => {
  return request({
    url: `/video/draft/${draftId}`,
    method: 'get'
  })
}


import request from '../utils/request.js'

/**
 * 保存草稿
 * @param {Object} data 草稿数据 {title, videoUrl}
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
 * @param {Object} data 视频数据 {draftId, title, videoUrl}
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


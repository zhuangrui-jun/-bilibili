import request from '../utils/request.js'

/**
 * 分页查询评论
 * @param {number} videoId 视频ID
 * @param {number} page 页码
 * @param {number} pageSize 每页数量
 * @returns {Promise}
 */
export const getCommentPage = (videoId, page = 1, pageSize = 10) => {
  return request({
    url: '/comment/page',
    method: 'get',
    params: { videoId, page, pageSize }
  })
}

/**
 * 发送评论（一级评论）
 * @param {Object} comment 评论对象 {refId, content}
 * @returns {Promise}
 */
export const addComment = (comment) => {
  return request({
    url: '/comment/add',
    method: 'post',
    data: comment
  })
}

/**
 * 回复评论
 * @param {Object} comment 评论对象 {refId, parentId, content}
 * @returns {Promise}
 */
export const replyComment = (comment) => {
  return request({
    url: '/comment/reply',
    method: 'post',
    data: comment
  })
}



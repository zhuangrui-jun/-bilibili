import request from '../utils/request.js'

/**
 * 发送邮箱验证码
 * @param {string} email 邮箱地址
 * @returns {Promise}
 */
export const sendCode = (email) => {
  return request({
    url: '/user/sendCode',
    method: 'post',
    params: { email }
  })
}

/**
 * 用户注册
 * @param {string} email 邮箱
 * @param {string} username 用户名
 * @param {string} password 密码
 * @param {string} code 验证码
 * @returns {Promise}
 */
export const register = (email, username, password, code) => {
  return request({
    url: '/user/register',
    method: 'post',
    params: { email, username, password, code }
  })
}

/**
 * 用户登录
 * @param {string} email 邮箱
 * @param {string} password 密码
 * @returns {Promise}
 */
export const login = (email, password) => {
  return request({
    url: '/user/login',
    method: 'post',
    params: { email, password }
  })
}


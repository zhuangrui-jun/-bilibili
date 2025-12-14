import axios from "axios";
import {ElMessage} from "element-plus";
import router from "../router/index.js";


const request = axios.create({
    baseURL: 'http://localhost:8081',
    timeout: 30000 // 后台接口超时时间
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    
    // 定义不需要token的接口路径
    const noAuthPaths = ['/user/login', '/user/register', '/user/sendCode', '/user/verifyCode'];
    const url = config.url || '';
    
    // 判断当前请求是否需要token
    const needAuth = !noAuthPaths.some(path => url.includes(path));
    
    if (needAuth) {
        // 从localStorage中获取token
        const user = JSON.parse(localStorage.getItem("user") || "{}");
        const token = user.token || localStorage.getItem("token");
        
        if (token) {
            config.headers['token'] = token;
        } else {
            // 如果没有token，跳转到登录页
            ElMessage.warning('请先登录');
            router.push('/login');
            return Promise.reject(new Error('未登录'));
        }
    }
    
    return config;
}, error => {
    return Promise.reject(error)
});
request.interceptors.response.use(response => {
        let res = response.data;
        // 兼容服务端返回的字符串数据
        if (typeof res == 'string') {
            res = res ? JSON.parse(res) : res
        }
        return res;
    },
    error => {
        if (error.response) {
            const status = error.response.status
            const res = error.response.data
            
            // 处理401未授权（token过期或无效）
            if (status === 401) {
                ElMessage.error(res?.msg || '登录已过期，请重新登录')
                // 清除本地存储的token和用户信息
                localStorage.removeItem('user')
                localStorage.removeItem('token')
                // 跳转到登录页
                router.push('/login')
                return Promise.reject(error)
            } else if (status === 404) {
                ElMessage.error('未找到请求接口')
            } else if (status === 500) {
                // 如果后端返回了错误信息，优先显示后端的错误信息
                ElMessage.error(res?.msg || '系统异常，请查看后端控制台报错')
            } else {
                // 其他错误状态码，尝试显示后端返回的错误信息
                ElMessage.error(res?.msg || error.message || '请求失败')
            }
            // 将错误响应数据附加到error对象上，方便组件中使用
            error.response.data = res
        } else {
            ElMessage.error('网络错误，请检查网络连接')
            console.error(error.message)
        }
        return Promise.reject(error)
    }
)
export default request
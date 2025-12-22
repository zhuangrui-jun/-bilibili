import { createRouter, createWebHistory } from 'vue-router'


const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            redirect: '/login'
        },
        {
            path: '/login',
            name: 'login',
            meta: {
                title: '登录',
                requireAuth: false
            },
            component: () => import('../views/Login.vue')
        },
        {
            path: '/test',
            name: 'first',
            meta: {
                title: "首页",
                requireAuth: true
            },
            component: () => import('../views/Test.vue')
        },
        {
            path: '/home',
            name: 'home',
            meta: {
                title: "主页",
                requireAuth: true
            },
            component: () => import('../views/Home.vue')
        },
        {
            path: '/banner',
            name: 'banner',
            meta: {
                title: "背景展示",
                requireAuth: true
            },
            component: () => import('../views/BannerBackground.vue')
        },
        {
            path: '/profile',
            name: 'profile',
            meta: {
                title: "个人主页",
                requireAuth: true
            },
            component: () => import('../views/Profile.vue')
        },
        {
            path: '/upload',
            name: 'upload',
            meta: {
                title: "投稿",
                requireAuth: true
            },
            component: () => import('../views/Upload.vue')
        },
        {
            path: '/video-submit/:draftId',
            name: 'videoSubmit',
            meta: {
                title: "完善视频信息",
                requireAuth: true
            },
            component: () => import('../views/VideoSubmit.vue')
        },
        {
            path: '/chat',
            name: 'chat',
            meta: {
                title: "聊天",
                requireAuth: true
            },
            component: () => import('../views/Chat.vue')
        },


    ],
})

// 路由前置守卫-没有token返回登录界面
router.beforeEach((to, from, next) => {
    // 设置页面标题
    if (to.meta.title) {
        document.title = to.meta.title;
    }
    
    // 1. 判断目标路由是否需要登录
    if (to.meta.requireAuth) {
        // 2. 检查本地是否有 token
        const user = JSON.parse(localStorage.getItem("user") || "{}");
        const token = user.token || localStorage.getItem('token');
        
        if (token) {
            // 有 token，允许跳转
            next();
        } else {
            // 无 token，强制跳转到登录页，并记录当前路径（方便登录后返回）
            next({ path: '/login', query: { redirect: to.fullPath } });
        }
    } else {
        // 不需要登录的路由，直接放行（如登录/注册页）
        next();
    }
})

export default router

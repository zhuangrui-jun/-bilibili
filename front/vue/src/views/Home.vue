<template>
  <div class="home-container">
    <!-- 头部区域 -->
    <header class="header">
      <!-- Banner 背景 -->
      <div class="header-background">
        <BannerBackground :banner-index="0" />
      </div>
      
      <!-- 头部内容 -->
      <div class="header-content">
        <div class="header-left">
          <div class="logo">B站</div>
          <nav class="nav-menu">
            <a href="#" class="nav-item">首页</a>
            <a href="#" class="nav-item">番剧</a>
            <a href="#" class="nav-item">直播</a>
            <a href="#" class="nav-item">游戏中心</a>
            <a href="#" class="nav-item">会员购</a>
            <a href="#" class="nav-item">漫画</a>
            <a href="#" class="nav-item">赛事</a>
          </nav>
        </div>
        
        <div class="header-center">
          <el-input
            v-model="searchText"
            placeholder="搜索视频、番剧、UP主"
            class="search-input"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="header-right">
          <el-avatar :size="40" :src="userAvatar" class="user-avatar" @click="goToProfile">
            <el-icon><User /></el-icon>
          </el-avatar>
          <el-button type="primary" class="upload-btn" @click="goToUpload">投稿</el-button>
        </div>
      </div>
    </header>

    <!-- 分区导航 -->
    <div class="category-nav">
      <div class="category-list">
        <div 
          v-for="category in categories" 
          :key="category.id"
          class="category-item"
          :class="{ active: activeCategory === category.id }"
          @click="activeCategory = category.id"
        >
          <el-icon v-if="category.icon" class="category-icon">
            <component :is="category.icon" />
          </el-icon>
          <span>{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 视频列表区域 -->
    <div class="main-content">
      <div class="video-grid">
        <!-- 走马灯轮播 -->
        <div class="carousel-card" v-if="carouselVideos.length > 0">
          <el-carousel 
            :interval="4000" 
            :arrow="carouselVideos.length > 1 ? 'hover' : 'never'"
            height="280px"
            indicator-position="outside"
          >
            <el-carousel-item 
              v-for="video in carouselVideos" 
              :key="video.id"
            >
              <div 
                class="carousel-item" 
                @click="goToVideo(video.id)"
              >
                <img 
                  :src="video.coverUrl || '/placeholder.jpg'" 
                  :alt="video.title"
                  class="carousel-image"
                  @error="handleImageError"
                />
                <div class="carousel-overlay">
                  <h3 class="carousel-title">{{ video.title }}</h3>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
        
        <!-- 普通视频卡片 -->
        <div 
          v-for="video in displayVideoList" 
          :key="video.id"
          class="video-card"
          @click="goToVideo(video.id)"
        >
          <div class="video-thumbnail">
            <img 
              :src="video.coverUrl || '/placeholder.jpg'" 
              :alt="video.title"
              @error="handleImageError"
            />
            <div class="video-play-overlay">
              <div class="play-button">
                <el-icon class="play-icon"><VideoPlay /></el-icon>
              </div>
            </div>
          </div>
          <div class="video-info">
            <h3 class="video-title" :title="video.title">{{ video.title }}</h3>
            <div class="video-meta">
              <span class="video-author" v-if="video.creatorName || video.creatorId">
                <span 
                  class="author-name" 
                  @click.stop="goToUserProfile(video.creatorId)"
                  :title="video.creatorName || '未知作者'"
                >
                  {{ video.creatorName || '未知作者' }}
                </span>
              </span>
              <span class="video-date">{{ formatDate(video.createdTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="4" animated />
      </div>
      
      <!-- 空状态 -->
      <div v-if="!loading && videoList.length === 0" class="empty-container">
        <el-empty description="暂无视频" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllVideos } from '../api/videoApi.js'
import { getUserDetail } from '../api/userApi.js'
import { ElMessage } from 'element-plus'
import { Search, User, VideoPlay } from '@element-plus/icons-vue'
import BannerBackground from './BannerBackground.vue'

const router = useRouter()

// 搜索文本
const searchText = ref('')

// 用户头像
const userAvatar = ref('')

// 分区列表
const categories = ref([
  { id: 'all', name: '全部', icon: null },
  { id: 'hot', name: '热门', icon: null },
  { id: 'anime', name: '番剧', icon: null },
  { id: 'game', name: '游戏', icon: null },
  { id: 'music', name: '音乐', icon: null },
  { id: 'dance', name: '舞蹈', icon: null },
  { id: 'tech', name: '科技', icon: null },
  { id: 'food', name: '美食', icon: null },
  { id: 'sports', name: '体育', icon: null },
  { id: 'entertainment', name: '娱乐', icon: null }
])

// 当前激活的分区
const activeCategory = ref('all')

// 视频列表
const videoList = ref([])

// 走马灯视频列表（取前5个）
const carouselVideos = ref([])

// 显示的视频列表（排除走马灯中的视频）
const displayVideoList = ref([])

// 加载状态
const loading = ref(false)

// 获取视频列表
const fetchVideos = async () => {
  loading.value = true
  try {
    const response = await getAllVideos()
    if (response && response.code === 200) {
      videoList.value = response.data
      // 设置走马灯视频（取前5个）
      carouselVideos.value = response.data.slice(0, 5)
      // 显示的视频列表（排除走马灯中的视频）
      displayVideoList.value = response.data.slice(5)
    } else {
      ElMessage.error(response?.msg || '获取视频列表失败')
    }
  } catch (error) {
    console.error('获取视频列表失败:', error)
    ElMessage.error('获取视频列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 图片加载错误处理
const handleImageError = (e) => {
  e.target.src = 'https://via.placeholder.com/300x200?text=No+Image'
}

// 跳转到视频播放页
const goToVideo = (videoId) => {
  router.push({
    path: '/test',
    query: { videoId: videoId }
  })
}

// 跳转到用户主页
const goToUserProfile = (userId) => {
  if (!userId) return
  router.push({
    path: '/profile',
    query: { userId: userId }
  })
}

// 跳转到投稿页面
const goToUpload = () => {
  router.push('/upload')
}

// 跳转到个人主页
const goToProfile = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (user.id) {
    router.push({
      path: '/profile',
      query: { userId: user.id }
    })
  } else {
    ElMessage.warning('用户信息不存在')
  }
}

// 获取用户头像
const fetchUserAvatar = async () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    console.log("user:",user)
    if (user.id) {
      const res = await getUserDetail(user.id)
      if (res.code === 200 && res.data) {
        // 如果后端返回了头像URL，使用它
        if (res.data.avatar) {
          userAvatar.value = res.data.avatar
        }
      }
    }
  } catch (error) {
    console.error('获取用户头像失败:', error)
  }
}

// 组件挂载时初始化
onMounted(() => {
  fetchVideos()
  fetchUserAvatar()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background-color: #f4f4f5;
}

/* 头部样式 */
.header {
  position: relative;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 155px;
  height: 10vw;
  max-height: 240px;
}

.header-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.header-background :deep(.banner-container) {
  width: 100%;
  height: 100%;
  margin: 0;
  min-width: unset;
}


.header-content {
  position: relative;
  z-index: 10;
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  cursor: pointer;
}

.nav-menu {
  display: flex;
  gap: 24px;
  align-items: center;
}

.nav-item {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.header-center {
  flex: 1;
  max-width: 500px;
}

.search-input {
  width: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-avatar {
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: transform 0.2s;
}

.user-avatar:hover {
  transform: scale(1.1);
}

.upload-btn {
  background-color: #fb7299;
  border-color: #fb7299;
  font-weight: 500;
  padding: 8px 20px;
}

.upload-btn:hover {
  background-color: #ff85ad;
  border-color: #ff85ad;
}

/* 分区导航样式 */
.category-nav {
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.category-list {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.category-list::-webkit-scrollbar {
  display: none;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 16px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  color: #606266;
  font-size: 14px;
}

.category-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.category-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.category-icon {
  font-size: 16px;
}

/* 主内容区域 */
.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 视频网格 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.video-card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.video-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  background-color: #f0f0f0;
  overflow: hidden;
  border-radius: 8px 8px 0 0;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.video-card:hover .video-thumbnail img {
  transform: scale(1.03);
}

.video-play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0);
  transition: background-color 0.3s ease;
  pointer-events: none;
}

.video-card:hover .video-play-overlay {
  background-color: rgba(0, 0, 0, 0.3);
}

.play-button {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.3s ease;
}

.video-card:hover .play-button {
  opacity: 1;
  transform: scale(1);
}

.play-icon {
  font-size: 24px;
  color: #fff;
  margin-left: 3px;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 40px;
}

.video-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}

.video-author {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #909399;
  flex: 1;
  min-width: 0;
}

.video-author::after {
  content: ' · ';
  margin: 0 6px;
  flex-shrink: 0;
}

.author-name {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-name:hover {
  color: #409eff;
}

.video-date {
  color: #909399;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 走马灯卡片 */
.carousel-card {
  grid-column: span 2;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.carousel-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.carousel-item {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.carousel-item:hover .carousel-image {
  transform: scale(1.05);
}

.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  padding: 20px;
  padding-top: 40px;
}

.carousel-title {
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

/* 加载状态 */
.loading-container {
  padding: 40px;
}

/* 空状态 */
.empty-container {
  padding: 80px 20px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .carousel-card {
    grid-column: span 3;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    padding: 12px 16px;
  }
  
  .header-left {
    width: 100%;
    justify-content: space-between;
  }
  
  .nav-menu {
    display: none;
  }
  
  .header-center {
    max-width: 100%;
    order: 3;
    width: 100%;
  }
  
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .carousel-card {
    grid-column: span 2;
  }
  
  .carousel-overlay {
    padding: 16px;
    padding-top: 30px;
  }
  
  .carousel-title {
    font-size: 14px;
  }
  
  .main-content {
    padding: 16px;
  }
}
</style>


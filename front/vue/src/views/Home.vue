<template>
  <div class="home-container">
    <!-- 头部区域 -->
    <header class="header">
      <div class="header-background"></div>
      
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
          <el-avatar :size="40" :src="userAvatar" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <el-button type="primary" class="upload-btn">投稿</el-button>
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
        <div 
          v-for="video in videoList" 
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
            <div class="video-duration" v-if="video.duration">
              {{ formatDuration(video.duration) }}
            </div>
            <div class="video-play-overlay">
              <el-icon class="play-icon"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="video-info">
            <h3 class="video-title" :title="video.title">{{ video.title }}</h3>
            <div class="video-meta">
              <span class="video-views">{{ formatViews(video.times) }}播放</span>
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
import { ElMessage } from 'element-plus'
import { Search, User, VideoPlay } from '@element-plus/icons-vue'

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

// 加载状态
const loading = ref(false)

// 获取视频列表
const fetchVideos = async () => {
  loading.value = true
  try {
    const response = await getAllVideos()
    if (response && response.code === 200) {
      videoList.value = response.data || []
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

// 格式化播放量
const formatViews = (views) => {
  if (!views) return '0'
  if (views < 10000) {
    return views.toString()
  } else if (views < 100000000) {
    return (views / 10000).toFixed(1) + '万'
  } else {
    return (views / 100000000).toFixed(1) + '亿'
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

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 图片加载错误处理
const handleImageError = (e) => {
  e.target.src = 'https://via.placeholder.com/300x200?text=No+Image'
}

// 跳转到视频详情页
const goToVideo = (videoId) => {
  router.push(`/video/${videoId}`)
}


// 组件挂载时初始化
onMounted(() => {
  fetchVideos()
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
  background: linear-gradient(180deg, #fb7299 0%, #ff6b9d 100%);
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(135deg, 
    rgba(251, 114, 153, 0.95) 0%, 
    rgba(255, 107, 157, 0.85) 30%,
    rgba(255, 133, 173, 0.9) 60%,
    rgba(251, 114, 153, 0.95) 100%
  );
  background-size: 200% 200%;
  animation: gradientShift 10s ease infinite;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}


.header-content {
  position: relative;
  z-index: 1;
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
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
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  background-color: #f0f0f0;
  overflow: hidden;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.video-card:hover .video-thumbnail img {
  transform: scale(1.05);
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.video-play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
  background-color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card:hover .video-play-overlay {
  opacity: 1;
}

.play-icon {
  font-size: 24px;
  color: #fff;
  margin-left: 4px;
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
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.video-views::after {
  content: ' · ';
  margin-left: 8px;
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
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }
  
  .main-content {
    padding: 16px;
  }
}
</style>


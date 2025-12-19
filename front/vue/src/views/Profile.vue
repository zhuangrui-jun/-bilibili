<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="header-background"></div>
      <div class="header-content">
        <el-button 
          type="text" 
          class="back-btn"
          @click="goBack"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>
    </div>

    <div class="profile-content" v-if="userInfo">
      <div class="user-card">
        <div class="avatar-section">
          <el-avatar 
            :size="120" 
            :src="userInfo.avatar || ''"
            class="user-avatar"
          >
            <el-icon><User /></el-icon>
          </el-avatar>
        </div>
        
        <div class="user-info-section">
          <h2 class="username">{{ userInfo.username || '未知用户' }}</h2>
          <div class="user-details">
            <div class="detail-item" v-if="userInfo.id">
              <span class="label">用户ID：</span>
              <span class="value">{{ userInfo.id }}</span>
            </div>
            <div class="detail-item" v-if="userInfo.email">
              <span class="label">邮箱：</span>
              <span class="value">{{ userInfo.email }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 导航标签 -->
      <div class="profile-tabs">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'videos' }"
          @click="activeTab = 'videos'"
        >
          投稿
        </div>
      </div>

      <!-- 视频列表 -->
      <div v-if="activeTab === 'videos'" class="videos-section">
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
        <div v-if="videoLoading" class="loading-container">
          <el-skeleton :rows="4" animated />
        </div>
        
        <!-- 空状态 -->
        <div v-if="!videoLoading && videoList.length === 0" class="empty-container">
          <el-empty description="暂无投稿视频" />
        </div>
        
        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="4" animated />
    </div>

    <!-- 错误状态 -->
    <div v-if="error && !loading" class="error-container">
      <el-empty description="加载用户信息失败" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserDetail } from '../api/userApi.js'
import { getVideoPage } from '../api/videoApi.js'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, VideoPlay } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const userInfo = ref(null)
const loading = ref(false)
const error = ref(false)

// 标签页
const activeTab = ref('videos')

// 视频列表
const videoList = ref([])
const videoLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 加载用户信息
const loadUserInfo = async (userId) => {
  loading.value = true
  error.value = false
  try {
    const response = await getUserDetail(userId)
    if (response && response.code === 200 && response.data) {
      userInfo.value = response.data
    } else {
      error.value = true
      ElMessage.error(response?.msg || '获取用户信息失败')
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    error.value = true
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取视频列表（分页）
const fetchVideos = async () => {
  const userId = route.query.userId
  if (!userId) return
  
  videoLoading.value = true
  try {
    const response = await getVideoPage({
      creatorId: userId,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    if (response && response.code === 200 && response.data) {
      videoList.value = response.data.records || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error(response?.msg || '获取视频列表失败')
    }
  } catch (error) {
    console.error('获取视频列表失败:', error)
    ElMessage.error('获取视频列表失败')
  } finally {
    videoLoading.value = false
  }
}

// 分页切换
const handlePageChange = (page) => {
  currentPage.value = page
  fetchVideos()
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知时间'
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
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

// 组件挂载
onMounted(() => {
  const userId = route.query.userId
  if (userId) {
    loadUserInfo(userId)
  } else {
    error.value = true
    ElMessage.error('缺少用户ID参数')
  }
  fetchVideos()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background-color: #f4f4f5;
}

.profile-header {
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
}

.back-btn {
  color: #fff;
  font-size: 16px;
  padding: 8px 16px;
}

.back-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.profile-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

.user-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.avatar-section {
  flex-shrink: 0;
}

.user-avatar {
  border: 4px solid #fb7299;
  background-color: #f0f0f0;
}

.user-info-section {
  flex: 1;
}

.username {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 24px 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.label {
  color: #909399;
  margin-right: 8px;
  min-width: 80px;
}

.value {
  color: #303133;
  font-weight: 500;
}

.loading-container,
.error-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

.error-container {
  text-align: center;
}

/* 导航标签 */
.profile-tabs {
  margin-top: 24px;
  background-color: #fff;
  border-radius: 12px;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  border-bottom: 2px solid #f0f0f0;
}

.tab-item {
  padding: 16px 24px;
  cursor: pointer;
  font-size: 16px;
  color: #606266;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.tab-item:hover {
  color: #fb7299;
}

.tab-item.active {
  color: #fb7299;
  border-bottom-color: #fb7299;
  font-weight: 500;
}

/* 视频列表区域 */
.videos-section {
  margin-top: 24px;
}

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
}

.video-author {
  flex: 1;
  overflow: hidden;
}

.author-name {
  cursor: pointer;
  transition: color 0.2s;
}

.author-name:hover {
  color: #fb7299;
}

.video-date {
  margin-left: 8px;
  white-space: nowrap;
}

.empty-container {
  padding: 80px 20px;
  text-align: center;
  background-color: #fff;
  border-radius: 12px;
  margin-top: 24px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .user-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px;
  }

  .user-details {
    width: 100%;
  }

  .detail-item {
    justify-content: center;
  }

  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>













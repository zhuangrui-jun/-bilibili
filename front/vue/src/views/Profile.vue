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
import { ElMessage } from 'element-plus'
import { ArrowLeft, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const userInfo = ref(null)
const loading = ref(false)
const error = ref(false)

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

// 组件挂载
onMounted(() => {
  const userId = route.query.userId
  if (userId) {
    loadUserInfo(userId)
  } else {
    error.value = true
    ElMessage.error('缺少用户ID参数')
  }
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

/* 响应式设计 */
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
}
</style>







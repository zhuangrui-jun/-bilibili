<template>
  <div class="chat-container">
    <div class="chat-header">
      <h2>聊天</h2>
      <div class="connection-status">
        <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
          {{ wsConnected ? '已连接' : '未连接' }}
        </el-tag>
      </div>
    </div>

    <div class="chat-content">
      <!-- 聊天消息列表 -->
      <div class="messages-area" ref="messagesArea">
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          class="message-item"
          :class="{ 'message-right': msg.isSelf }"
        >
          <div class="message-content">
            <div class="message-header">
              <span class="message-user">{{ msg.isSelf ? '我' : '好友' }}</span>
              <span class="message-time">{{ formatTime(msg.time) }}</span>
            </div>
            <div class="message-text">{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="messages.length === 0" class="empty-message">
          暂无消息，开始聊天吧~
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          @keyup.ctrl.enter="sendMessage"
          :disabled="!wsConnected"
        />
        <div class="input-actions">
          <el-button 
            type="primary" 
            @click="sendMessage"
            :disabled="!inputMessage.trim() || !wsConnected"
          >
            发送 (Ctrl+Enter)
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// 固定好友ID（测试用）
const FRIEND_USER_ID = '2002590444048060418'

// WebSocket连接
let ws = null
const wsConnected = ref(false)

// 当前用户ID
const currentUserId = ref('')

// 消息列表
const messages = ref([])
const inputMessage = ref('')
const messagesArea = ref(null)

// 获取当前用户ID
const getCurrentUserId = () => {
  const user = JSON.parse(localStorage.getItem("user") || "{}")
  // UserVO中的id字段是Long类型，需要转换为字符串
  return user.id ? String(user.id) : ''
}

// 初始化WebSocket连接
const initWebSocket = () => {
  currentUserId.value = getCurrentUserId()
  
  if (!currentUserId.value) {
    ElMessage.error('请先登录')
    return
  }

  // WebSocket连接URL（根据后端代码，需要通过查询参数传递userId）
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = 'localhost:8081'
  const wsUrl = `${wsProtocol}//${wsHost}/chat?userId=${currentUserId.value}`

  try {
    ws = new WebSocket(wsUrl)

    // 连接成功
    ws.onopen = () => {
      wsConnected.value = true
      ElMessage.success('WebSocket连接成功')
      console.log('WebSocket连接已建立')
    }

    // 接收消息
    ws.onmessage = (event) => {
      try {
        const response = JSON.parse(event.data)
        console.log('收到消息:', response)
        
        // 添加消息到列表
        messages.value.push({
          content: response.content,
          time: response.time || Date.now(),
          isSelf: response.userId === currentUserId.value,
          userId: response.userId
        })
        
        // 滚动到底部
        scrollToBottom()
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }

    // 连接关闭
    ws.onclose = () => {
      wsConnected.value = false
      ElMessage.warning('WebSocket连接已断开')
      console.log('WebSocket连接已关闭')
      
      // 尝试重连
      setTimeout(() => {
        if (!wsConnected.value) {
          console.log('尝试重新连接...')
          initWebSocket()
        }
      }, 3000)
    }

    // 连接错误
    ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
      ElMessage.error('WebSocket连接错误')
      wsConnected.value = false
    }

  } catch (error) {
    console.error('创建WebSocket连接失败:', error)
    ElMessage.error('创建WebSocket连接失败')
  }
}

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim() || !wsConnected.value) {
    return
  }

  const messageData = {
    userId: currentUserId.value,
    toUserId: FRIEND_USER_ID,
    content: inputMessage.value.trim()
  }

  try {
    ws.send(JSON.stringify(messageData))
    
    // 立即显示自己发送的消息（前端预览）
    messages.value.push({
      content: messageData.content,
      time: Date.now(),
      isSelf: true,
      userId: currentUserId.value
    })
    
    inputMessage.value = ''
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesArea.value) {
      messagesArea.value.scrollTop = messagesArea.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

// 关闭WebSocket连接
const closeWebSocket = () => {
  if (ws) {
    ws.close()
    ws = null
  }
}

onMounted(() => {
  initWebSocket()
})

onBeforeUnmount(() => {
  closeWebSocket()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  width: 100%;
}

.message-item.message-right {
  justify-content: flex-end;
}

.message-content {
  max-width: 70%;
  background-color: #f0f0f0;
  border-radius: 8px;
  padding: 12px;
}

.message-right .message-content {
  background-color: #409eff;
  color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
  opacity: 0.8;
}

.message-user {
  font-weight: 500;
}

.message-time {
  margin-left: 8px;
}

.message-text {
  word-wrap: break-word;
  line-height: 1.5;
}

.empty-message {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}

.input-area {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  gap: 12px;
}

/* 滚动条样式 */
.messages-area::-webkit-scrollbar {
  width: 6px;
}

.messages-area::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>


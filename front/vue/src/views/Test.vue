<template>
  <div class="player-container">
    <div class="player-wrapper">
      <!-- Plyr 播放器容器 -->
      <div class="plyr-wrapper">
        <video
          ref="videoElement"
          id="plyr-player"
          class="plyr-player"
          controls
          playsinline
        >
          <source :src="videoUrl" type="video/mp4" />
        </video>
      </div>
      
      <!-- Danmaku 弹幕容器（覆盖在播放器上方） -->
      <div ref="danmakuContainer" class="danmaku-container"></div>
    </div>

    <!-- 弹幕输入区域 -->
    <div class="danmaku-input-area">
      <div class="danmaku-input-wrapper">
        <!-- 弹幕开关按钮 -->
        <el-button
          class="danmaku-toggle-btn"
          :type="danmakuEnabled ? 'primary' : 'default'"
          :icon="danmakuEnabled ? ChatDotRound : ChatLineRound"
          circle
          @click="toggleDanmaku"
          :title="danmakuEnabled ? '关闭弹幕' : '开启弹幕'"
        />
        
        <!-- 弹幕设置按钮（颜色和位置） -->
        <el-popover
          placement="top"
          :width="320"
          trigger="click"
          popper-class="danmaku-settings-popover"
        >
          <template #reference>
            <el-button
              class="danmaku-settings-btn"
              :icon="Setting"
              circle
              title="弹幕设置"
            />
          </template>
          
          <div class="danmaku-settings-content">
            <div class="settings-section">
              <div class="settings-title">弹幕位置</div>
              <div class="type-options">
                <div
                  v-for="type in danmakuTypes"
                  :key="type.value"
                  class="type-item"
                  :class="{ active: danmakuType === type.value }"
                  @click="selectType(type.value)"
                >
                  <el-icon><component :is="type.icon" /></el-icon>
                  <span>{{ type.label }}</span>
                </div>
              </div>
            </div>
            
            <div class="settings-section">
              <div class="settings-title">弹幕颜色</div>
              <div class="color-options-grid">
                <div
                  v-for="color in presetColors"
                  :key="color.value"
                  class="color-item"
                  :class="{ active: danmakuColor === color.value }"
                  :style="{ backgroundColor: color.value }"
                  @click="selectColor(color.value)"
                  :title="color.name"
                ></div>
              </div>
            </div>
          </div>
        </el-popover>
        
        <!-- 输入框 -->
        <el-input
          v-model="danmakuText"
          placeholder="发条友善的弹幕见证下"
          class="danmaku-input"
          @keyup.enter="sendDanmaku"
          clearable
        />
        
        <!-- 发送按钮 -->
        <el-button
          type="primary"
          class="send-btn"
          @click="sendDanmaku"
          :disabled="!danmakuText.trim()"
        >
          发送
        </el-button>
      </div>
    </div>

    <!-- 评论区域 -->
    <div class="comment-section">
      <div class="comment-header">
        <h3>评论区</h3>
        <el-button 
          type="primary" 
          size="small"
          @click="showCommentInput = !showCommentInput"
        >
          {{ showCommentInput ? '取消' : '发表评论' }}
        </el-button>
      </div>

      <!-- 评论输入框 -->
      <div v-if="showCommentInput" class="comment-input-area">
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="3"
          placeholder="发条友善的评论吧~"
          maxlength="500"
          show-word-limit
        />
        <div class="comment-input-actions">
          <el-button @click="cancelComment">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitComment"
            :disabled="!commentText.trim()"
          >
            发表
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comment-list" v-loading="commentLoading">
        <div v-if="comments.length === 0 && !commentLoading" class="empty-comment">
          暂无评论，快来发表第一条评论吧~
        </div>
        
        <div 
          v-for="comment in comments" 
          :key="comment.id"
          class="comment-item"
        >
          <!-- 一级评论 -->
          <div class="comment-main">
            <div class="comment-avatar">
              <el-avatar 
                :src="comment.user?.avatar" 
                :size="40"
              >
                {{ comment.user?.username?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <div class="comment-content">
              <div class="comment-header-info">
                <span class="comment-username">{{ comment.user?.username || '匿名用户' }}</span>
                <span class="comment-time">{{ formatTime(comment.createdTime) }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-actions">
                <el-button 
                  text 
                  type="primary" 
                  size="small"
                  @click="toggleReply(comment.id)"
                >
                  {{ replyInputMap[comment.id] ? '取消回复' : '回复' }}
                </el-button>
              </div>
            </div>
          </div>

          <!-- 回复输入框 -->
          <div v-if="replyInputMap[comment.id]" class="reply-input-area">
            <el-input
              v-model="replyTextMap[comment.id]"
              type="textarea"
              :rows="2"
              :placeholder="`回复 ${comment.user?.username || '用户'}`"
              maxlength="300"
              show-word-limit
            />
            <div class="reply-input-actions">
              <el-button size="small" @click="cancelReply(comment.id)">取消</el-button>
              <el-button 
                type="primary" 
                size="small"
                @click="submitReply(comment.id, comment)"
                :disabled="!replyTextMap[comment.id]?.trim()"
              >
                回复
              </el-button>
            </div>
          </div>

          <!-- 子评论（回复）列表 -->
          <div v-if="comment.children && comment.children.length > 0" class="comment-replies">
            <div 
              v-for="reply in comment.children" 
              :key="reply.id"
              class="reply-item"
            >
              <div class="reply-avatar">
                <el-avatar 
                  :src="reply.user?.avatar" 
                  :size="32"
                >
                  {{ reply.user?.username?.charAt(0) || 'U' }}
                </el-avatar>
              </div>
              <div class="reply-content">
                <div class="reply-header-info">
                  <span class="reply-username">{{ reply.user?.username || '匿名用户' }}</span>
                  <span v-if="reply.replyToUser" class="reply-to">
                    回复 
                    <span class="reply-to-user">@{{ reply.replyToUser.username }}</span>
                  </span>
                  <span class="reply-time">{{ formatTime(reply.createdTime) }}</span>
                </div>
                <div class="reply-text">{{ reply.content }}</div>
                <div class="reply-actions">
                  <el-button 
                    text 
                    type="primary" 
                    size="small"
                    @click="toggleReplyToReply(comment.id, reply.id)"
                  >
                    {{ replyInputMap[`${comment.id}-${reply.id}`] ? '取消回复' : '回复' }}
                  </el-button>
                </div>

                <!-- 回复的回复输入框 -->
                <div v-if="replyInputMap[`${comment.id}-${reply.id}`]" class="reply-input-area">
                  <el-input
                    v-model="replyTextMap[`${comment.id}-${reply.id}`]"
                    type="textarea"
                    :rows="2"
                    :placeholder="`回复 ${reply.user?.username || '用户'}`"
                    maxlength="300"
                    show-word-limit
                  />
                  <div class="reply-input-actions">
                    <el-button size="small" @click="cancelReply(`${comment.id}-${reply.id}`)">取消</el-button>
                    <el-button 
                      type="primary" 
                      size="small"
                      @click="submitReplyToReply(comment.id, reply.id, reply)"
                      :disabled="!replyTextMap[`${comment.id}-${reply.id}`]?.trim()"
                    >
                      回复
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="commentTotal > 0" class="comment-pagination">
          <el-pagination
            v-model:current-page="commentPage"
            v-model:page-size="commentPageSize"
            :total="commentTotal"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadComments"
            @current-change="loadComments"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import Danmaku from 'danmaku'
import { getDanmakuList, sendDanmaku as sendDanmakuApi } from '../api/danmakuApi.js'
import { getVideoDetail } from '../api/videoApi.js'
import { getCommentPage, addComment, replyComment } from '../api/commentApi.js'
import { ElMessage } from 'element-plus'
import { 
  ChatDotRound, 
  ChatLineRound, 
  Setting,
  Sort,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()

// 视频配置
const videoUrl = ref('')
const videoId = ref('') // 视频ID，用于弹幕池
const videoInfo = ref(null) // 视频详细信息

// 播放器实例
const videoElement = ref(null)
const danmakuContainer = ref(null)
let plyrInstance = null
let danmakuInstance = null

// 弹幕相关
const danmakuText = ref('')
const danmakuType = ref(0) // 0: 滚动, 1: 顶部, 2: 底部
const danmakuColor = ref('#ffffff')
const danmakuEnabled = ref(true)

// 预设颜色选项
const presetColors = [
  { name: '白色', value: '#ffffff' },
  { name: '红色', value: '#ff0000' },
  { name: '橙色', value: '#ff8c00' },
  { name: '黄色', value: '#ffff00' },
  { name: '绿色', value: '#00ff00' },
  { name: '青色', value: '#00ffff' },
  { name: '蓝色', value: '#0000ff' },
  { name: '紫色', value: '#ff00ff' },
  { name: '粉色', value: '#ff69b4' },
  { name: '金色', value: '#ffd700' }
]

// 弹幕类型选项
const danmakuTypes = [
  { label: '滚动', value: 0, icon: Sort },
  { label: '顶部', value: 1, icon: ArrowUp },
  { label: '底部', value: 2, icon: ArrowDown }
]

// 评论相关
const showCommentInput = ref(false)
const commentText = ref('')
const comments = ref([])
const commentLoading = ref(false)
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)
const replyInputMap = ref({}) // 控制回复输入框显示
const replyTextMap = ref({}) // 存储回复内容

// 选择颜色
const selectColor = (color) => {
  danmakuColor.value = color
}

// 选择弹幕类型
const selectType = (type) => {
  danmakuType.value = type
}

// 存储已加载的弹幕数据
const danmakuData = ref([])

// 初始化播放器
const initPlayer = async () => {
  if (!videoElement.value || !danmakuContainer.value) return

  // 初始化 Plyr 播放器
  plyrInstance = new Plyr('#plyr-player', {
    controls: [
      'play-large',
      'play',
      'progress',
      'current-time',
      'mute',
      'volume',
      'settings',
      'pip',
      'airplay',
      'fullscreen'
    ],
    settings: ['captions', 'quality', 'speed'],
    keyboard: { focused: true, global: false },
    tooltips: { controls: true, seek: true },
    autoplay: false,
    clickToPlay: true
  })

  // 初始化 Danmaku 弹幕系统
  danmakuInstance = new Danmaku({
    container: danmakuContainer.value,
    media: videoElement.value,
    speed: 144, // 弹幕速度
    opacity: 0.8, // 弹幕透明度
    fontSize: 20, // 字体大小
    unlimited: true // 无限弹幕
  })

  // 同步播放器状态
  syncPlayerState()

  // 加载弹幕
  await loadDanmaku()
}

// 同步播放器状态
const syncPlayerState = () => {
  if (!plyrInstance || !danmakuInstance) return

  // 监听 Plyr 播放事件，同步弹幕时间
  plyrInstance.on('timeupdate', () => {
    if (danmakuInstance && plyrInstance) {
      const currentTime = plyrInstance.currentTime
      // Danmaku 库会自动同步视频时间，但我们需要手动触发弹幕显示
      updateDanmakuTime(currentTime)
    }
  })

  // 监听播放/暂停
  // 注意：danmaku 库已经通过 media 参数绑定了视频元素，会自动同步播放状态
  // 不需要手动调用 play() 和 pause() 方法
  plyrInstance.on('play', () => {
    // danmaku 库会自动处理播放状态
    if (danmakuInstance && plyrInstance) {
      const currentTime = plyrInstance.currentTime || 0
      updateDanmakuTime(currentTime)
    }
  })

  plyrInstance.on('pause', () => {
    // danmaku 库会自动处理暂停状态
  })

  // 监听进度变化（拖拽进度条）
  plyrInstance.on('seeked', () => {
    if (danmakuInstance && plyrInstance) {
      const currentTime = plyrInstance.currentTime
      // 重置已显示的弹幕状态，以便重新显示
      danmakuData.value.forEach(danmaku => {
        // 如果弹幕时间在当前时间之后，标记为未显示
        if (danmaku[0] > currentTime) {
          danmaku._shown = false
        }
      })
      // danmaku 库会自动同步视频时间，我们只需要更新弹幕显示
      updateDanmakuTime(currentTime)
    }
  })
}

// 更新弹幕时间
const updateDanmakuTime = (currentTime) => {
  if (!danmakuInstance || !danmakuData.value.length) return

  // 显示当前时间应该显示的弹幕
  danmakuData.value.forEach(danmaku => {
    // 弹幕格式：[time, type, color, author, text]
    const danmakuTime = danmaku[0]
    const timeDiff = currentTime - danmakuTime
    
    // 如果弹幕时间在当前位置附近（0.1秒内），且还未显示，则显示
    if (timeDiff >= 0 && timeDiff < 0.1 && !danmaku._shown) {
      const type = danmaku[1] // 0: 滚动, 1: 顶部, 2: 底部
      const colorDecimal = danmaku[2]
      // 转换为十六进制颜色（确保是6位）
      const colorHex = colorDecimal.toString(16).padStart(6, '0')
      const color = `#${colorHex}`
      const text = danmaku[4]
      
      // 根据类型发送弹幕
      const danmakuTypeMap = {
        0: 'scroll', // 滚动
        1: 'top',    // 顶部
        2: 'bottom'  // 底部
      }
      
      try {
        danmakuInstance.emit({
          text: text,
          style: {
            color: color,
            fontSize: '20px',
            fontWeight: 'bold'
          },
          mode: danmakuTypeMap[type] || 'scroll'
        })
        
        danmaku._shown = true
      } catch (error) {
        console.error('发送弹幕失败:', error)
      }
    }
  })
}

// 加载弹幕
const loadDanmaku = async () => {
  try {
    const response = await getDanmakuList(videoId.value)
    if (response && response.code === 0 && response.data) {
      danmakuData.value = response.data
      console.log('弹幕加载成功', response.data.length)
      
      // 重置所有弹幕的显示状态
      danmakuData.value.forEach(danmaku => {
        danmaku._shown = false
      })
    }
  } catch (error) {
    console.error('加载弹幕失败:', error)
    ElMessage.error('加载弹幕失败')
  }
}

// 发送弹幕
const sendDanmaku = async () => {
  if (!danmakuText.value.trim()) {
    ElMessage.warning('请输入弹幕内容')
    return
  }

  if (!plyrInstance || !danmakuInstance) {
    ElMessage.warning('播放器未初始化')
    return
  }

  const currentTime = plyrInstance.currentTime || 0

  // 转换颜色格式（从 hex 转为十进制）
  // 确保颜色值是字符串格式
  let colorValue = danmakuColor.value
  
  // 处理可能的对象格式（Element Plus ColorPicker 可能返回对象）
  if (typeof colorValue === 'object' && colorValue !== null) {
    // 如果是对象，尝试获取 hex 属性
    if (colorValue.hex) {
      colorValue = colorValue.hex
    } else if (colorValue.rgba) {
      // 如果有 rgba，转换为 hex
      const r = colorValue.rgba.r.toString(16).padStart(2, '0')
      const g = colorValue.rgba.g.toString(16).padStart(2, '0')
      const b = colorValue.rgba.b.toString(16).padStart(2, '0')
      colorValue = `#${r}${g}${b}`
    } else {
      // 默认白色
      colorValue = '#ffffff'
    }
  }
  
  // 确保是字符串格式
  if (typeof colorValue !== 'string') {
    colorValue = '#ffffff'
  }
  
  // 移除 # 号并转换为十进制
  let colorHex = colorValue.replace('#', '').trim()
  
  // 如果颜色值无效，使用默认白色
  if (!colorHex || colorHex.length !== 6) {
    console.warn('颜色值无效，使用默认白色:', colorValue)
    colorHex = 'ffffff'
    colorValue = '#ffffff'
  }
  
  // 转换为十进制 RGB 值
  let colorDecimal = parseInt(colorHex, 16)
  
  // 验证转换结果，如果失败则使用默认白色
  if (isNaN(colorDecimal) || colorDecimal < 0 || colorDecimal > 0xffffff) {
    console.warn('颜色转换失败，使用默认白色:', colorValue)
    colorHex = 'ffffff'
    colorValue = '#ffffff'
    colorDecimal = parseInt('ffffff', 16)
  }

  // 获取颜色字符串（用于 Danmaku 显示）
  const colorString = colorValue.startsWith('#') ? colorValue : `#${colorValue}`

  // 转换弹幕类型：0=滚动(scroll), 1=顶部(top), 2=底部(bottom)
  const danmakuTypeMap = {
    0: 'scroll',
    1: 'top',
    2: 'bottom'
  }
  const danmakuMode = danmakuTypeMap[danmakuType.value] || 'scroll'

  // 获取当前用户信息
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const authorName = user.username || user.id || 'User'

  // 构建发送数据，确保所有字段都有值
  const danmakuDataToSend = {
    id: videoId.value || 'test-video-001',
    author: authorName,
    time: currentTime,
    text: danmakuText.value.trim(),
    color: colorDecimal,
    type: danmakuType.value
  }
  
  // 验证数据完整性
  if (!danmakuDataToSend.id || !danmakuDataToSend.text || 
      danmakuDataToSend.color === null || danmakuDataToSend.color === undefined ||
      danmakuDataToSend.type === null || danmakuDataToSend.type === undefined) {
    console.error('弹幕数据不完整:', danmakuDataToSend)
    ElMessage.error('弹幕数据格式错误，请重试')
    return
  }

  try {
    // 先发送到后端
    await sendDanmakuApi(danmakuDataToSend)
    
    // 然后立即在播放器中显示弹幕
    if (danmakuInstance) {
      try {
        danmakuInstance.emit({
          text: danmakuText.value,
          style: {
            color: colorString,
            fontSize: '20px',
            fontWeight: 'bold'
          },
          mode: danmakuMode
        })
      } catch (error) {
        console.error('显示弹幕失败:', error)
      }
    }

    // 添加到本地弹幕数据
    danmakuData.value.push([
      currentTime,
      danmakuType.value,
      colorDecimal,
      authorName,
      danmakuText.value
    ])

    danmakuText.value = ''
    ElMessage.success('弹幕发送成功')
  } catch (error) {
    console.error('发送弹幕失败:', error)
    ElMessage.error('发送弹幕失败: ' + (error.message || '未知错误'))
  }
}

// 切换弹幕显示
const toggleDanmaku = () => {
  danmakuEnabled.value = !danmakuEnabled.value
  // 通过 CSS 控制弹幕容器的显示/隐藏
  if (danmakuContainer.value) {
    if (danmakuEnabled.value) {
      danmakuContainer.value.style.display = 'block'
      danmakuContainer.value.style.opacity = '1'
    } else {
      danmakuContainer.value.style.opacity = '0'
      // 延迟隐藏，让动画更流畅
      setTimeout(() => {
        if (!danmakuEnabled.value && danmakuContainer.value) {
          danmakuContainer.value.style.display = 'none'
        }
      }, 300)
    }
  }
}

// 监听视频 URL 变化
watch(videoUrl, () => {
  if (plyrInstance) {
    plyrInstance.source = {
      type: 'video',
      sources: [
        {
          src: videoUrl.value,
          type: 'video/mp4'
        }
      ]
    }
  }
})

// 加载视频详情
const loadVideoDetail = async (id) => {
  try {
    const response = await getVideoDetail(id)
    if (response && response.code === 200 && response.data) {
      videoInfo.value = response.data
      // 默认使用url7（720p），如果没有则使用url1（1080p），最后使用url4（480p）
      const url = response.data.videoUrl7 || response.data.videoUrl1 || response.data.videoUrl4 || ''
      if (!url) {
        ElMessage.error('视频播放地址不存在')
        return
      }
      videoUrl.value = url
      videoId.value = String(response.data.id) // 确保videoId是字符串格式
      
      // 加载完视频信息后初始化播放器
      await nextTick()
      setTimeout(() => {
        initPlayer()
      }, 100)
    } else {
      ElMessage.error(response?.msg || '获取视频详情失败')
    }
  } catch (error) {
    console.error('加载视频详情失败:', error)
    ElMessage.error('加载视频详情失败')
  }
}

// 加载评论
const loadComments = async () => {
  if (!videoId.value) return
  
  commentLoading.value = true
  try {
    const response = await getCommentPage(videoId.value, commentPage.value, commentPageSize.value)
    if (response && response.code === 200 && response.data) {
      comments.value = response.data.records || []
      commentTotal.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  } finally {
    commentLoading.value = false
  }
}

// 提交评论（一级评论）
const submitComment = async () => {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  if (!videoId.value) {
    ElMessage.error('视频ID不存在')
    return
  }

  try {
    await addComment({
      refId: Number(videoId.value),
      content: commentText.value.trim()
    })
    ElMessage.success('评论发表成功')
    commentText.value = ''
    showCommentInput.value = false
    // 重新加载评论（回到第一页）
    commentPage.value = 1
    await loadComments()
  } catch (error) {
    console.error('发表评论失败:', error)
    ElMessage.error(error.message || '发表评论失败')
  }
}

// 取消评论
const cancelComment = () => {
  commentText.value = ''
  showCommentInput.value = false
}

// 切换回复输入框
const toggleReply = (commentId) => {
  if (replyInputMap.value[commentId]) {
    delete replyInputMap.value[commentId]
    delete replyTextMap.value[commentId]
  } else {
    // 关闭其他回复输入框
    Object.keys(replyInputMap.value).forEach(key => {
      if (key.startsWith(`${commentId}-`)) {
        delete replyInputMap.value[key]
        delete replyTextMap.value[key]
      }
    })
    replyInputMap.value[commentId] = true
    replyTextMap.value[commentId] = ''
  }
}

// 切换回复的回复输入框
const toggleReplyToReply = (commentId, replyId) => {
  const key = `${commentId}-${replyId}`
  if (replyInputMap.value[key]) {
    delete replyInputMap.value[key]
    delete replyTextMap.value[key]
  } else {
    // 关闭其他回复输入框
    Object.keys(replyInputMap.value).forEach(k => {
      if (k !== key && (k === String(commentId) || k.startsWith(`${commentId}-`))) {
        delete replyInputMap.value[k]
        delete replyTextMap.value[k]
      }
    })
    replyInputMap.value[key] = true
    replyTextMap.value[key] = ''
  }
}

// 提交回复
const submitReply = async (commentId, parentComment) => {
  const replyText = replyTextMap.value[commentId]
  if (!replyText || !replyText.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  if (!videoId.value) {
    ElMessage.error('视频ID不存在')
    return
  }

  try {
    await replyComment({
      refId: Number(videoId.value),
      parentId: commentId,
      content: replyText.trim()
    })
    ElMessage.success('回复成功')
    delete replyInputMap.value[commentId]
    delete replyTextMap.value[commentId]
    // 重新加载评论
    await loadComments()
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error(error.message || '回复失败')
  }
}

// 提交回复的回复
const submitReplyToReply = async (commentId, replyId, parentReply) => {
  const key = `${commentId}-${replyId}`
  const replyText = replyTextMap.value[key]
  if (!replyText || !replyText.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  if (!videoId.value) {
    ElMessage.error('视频ID不存在')
    return
  }

  try {
    // 回复的回复，parentId是回复的ID
    await replyComment({
      refId: Number(videoId.value),
      parentId: replyId,
      content: replyText.trim()
    })
    ElMessage.success('回复成功')
    delete replyInputMap.value[key]
    delete replyTextMap.value[key]
    // 重新加载评论
    await loadComments()
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error(error.message || '回复失败')
  }
}

// 取消回复
const cancelReply = (key) => {
  delete replyInputMap.value[key]
  delete replyTextMap.value[key]
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于24小时
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  // 格式化日期
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  
  if (year === now.getFullYear()) {
    return `${month}-${day} ${hour}:${minute}`
  }
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 组件挂载
onMounted(async () => {
  // 从路由参数获取视频ID
  const id = route.query.videoId
  if (id) {
    await loadVideoDetail(id)
    // 加载评论
    await loadComments()
  } else {
    ElMessage.error('缺少视频ID参数')
  }
})

// 组件卸载
onBeforeUnmount(() => {
  if (plyrInstance) {
    plyrInstance.destroy()
  }
  if (danmakuInstance) {
    danmakuInstance.destroy()
  }
})
</script>

<style scoped>
.player-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.player-wrapper {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.plyr-wrapper {
  position: relative;
  width: 100%;
  z-index: 1;
}

.plyr-player {
  width: 100%;
  height: auto;
  display: block;
}

.danmaku-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
  overflow: hidden;
}

.danmaku-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.danmaku-input-wrapper {
  display: flex;
  gap: 8px;
  align-items: center;
}

.danmaku-toggle-btn,
.danmaku-settings-btn {
  flex-shrink: 0;
}

.danmaku-input {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
  min-width: 80px;
}

/* 弹幕设置弹窗样式 */
:deep(.danmaku-settings-popover) {
  padding: 16px;
}

.danmaku-settings-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.settings-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.type-options {
  display: flex;
  gap: 8px;
}

.type-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  color: #606266;
}

.type-item:hover {
  border-color: #409eff;
  color: #409eff;
  background-color: #ecf5ff;
}

.type-item.active {
  border-color: #409eff;
  background-color: #ecf5ff;
  color: #409eff;
}

.type-item .el-icon {
  font-size: 20px;
}

.type-item span {
  font-size: 12px;
}

.color-options-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.color-item {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.color-item:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.color-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  transform: scale(1.1);
}

/* 评论区域样式 */
.comment-section {
  margin-top: 30px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.comment-input-area {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.comment-list {
  min-height: 200px;
}

.empty-comment {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.comment-item {
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.comment-username {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.comment-time {
  color: #909399;
  font-size: 12px;
}

.comment-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 15px;
}

.comment-replies {
  margin-top: 15px;
  margin-left: 52px;
  padding-left: 15px;
  border-left: 2px solid #ebeef5;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-content {
  flex: 1;
}

.reply-header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.reply-username {
  font-weight: 500;
  color: #303133;
  font-size: 13px;
}

.reply-to {
  color: #909399;
  font-size: 12px;
}

.reply-to-user {
  color: #409eff;
  cursor: pointer;
}

.reply-to-user:hover {
  text-decoration: underline;
}

.reply-time {
  color: #909399;
  font-size: 12px;
}

.reply-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 6px;
  word-break: break-word;
}

.reply-actions {
  display: flex;
  gap: 10px;
}

.reply-input-area {
  margin-top: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.comment-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>

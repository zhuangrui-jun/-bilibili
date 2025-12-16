<template>
  <div class="upload-container">
    <div class="upload-content">
      <h2 class="upload-title">投稿</h2>
      
      <!-- 视频上传区域 -->
      <div class="upload-section">
        <div class="section-label">视频文件</div>
        <el-upload
          ref="uploadRef"
          class="video-uploader"
          :auto-upload="false"
          :on-change="handleVideoChange"
          :on-remove="handleVideoRemove"
          :limit="1"
          accept="video/mp4"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将视频文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传mp4格式的视频文件
            </div>
          </template>
        </el-upload>
        
        <!-- 视频预览 -->
        <div v-if="videoPreviewUrl" class="video-preview">
          <video :src="videoPreviewUrl" controls style="max-width: 100%; max-height: 400px;"></video>
        </div>
        
        <!-- 上传进度 -->
        <div v-if="uploadProgress > 0" class="upload-progress">
          <el-progress :percentage="uploadProgress" :status="uploadStatus" />
          <div class="progress-text">
            <span v-if="uploadProgress < 100">上传中... {{ uploadProgress }}%</span>
            <span v-else>上传完成！</span>
          </div>
        </div>
      </div>

      <!-- 标题输入 -->
      <div class="upload-section">
        <div class="section-label">视频标题</div>
        <el-input
          v-model="videoTitle"
          placeholder="请输入视频标题"
          maxlength="100"
          show-word-limit
          clearable
        />
      </div>

      <!-- 操作按钮 -->
      <div class="upload-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="info" @click="handleSaveDraft" :loading="savingDraft">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="!canSubmit">
          提交
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { saveDraft, submitVideo, getDraftDetail } from '../api/uploadApi.js'
import OSS from 'ali-oss'

const router = useRouter()

// 上传组件引用
const uploadRef = ref(null)

// 视频文件
const videoFile = ref(null)
const videoPreviewUrl = ref('')

// 上传进度
const uploadProgress = ref(0)
const uploadStatus = ref('')

// 视频标题
const videoTitle = ref('')

// 草稿ID（如果有）
const draftId = ref(null)

// 保存草稿状态
const savingDraft = ref(false)

// 提交状态
const submitting = ref(false)

// OSS客户端
let ossClient = null

// 已上传的视频URL
const uploadedVideoUrl = ref('')

// 初始化OSS客户端
const initOssClient = () => {
  const region = import.meta.env.VITE_OSS_REGION 
  const accessKeyId = import.meta.env.VITE_OSS_ACCESS_KEY_ID
  const accessKeySecret = import.meta.env.VITE_OSS_ACCESS_KEY_SECRET
  const bucket = import.meta.env.VITE_OSS_BUCKET || 'zhangrui123'

  if (!accessKeyId || !accessKeySecret || !bucket) {
    ElMessage.error('OSS配置不完整，请检查环境变量')
    return null
  }

  // 构建正确的endpoint格式：{region}.aliyuncs.com
  const endpoint = `${region}.aliyuncs.com`

  console.log('OSS配置:', { region, bucket, endpoint })

  return new OSS({
    region: region,
    accessKeyId: accessKeyId,
    accessKeySecret: accessKeySecret,
    bucket: bucket,
    endpoint: endpoint, // 显式设置endpoint，确保格式正确
    secure: true
  })
}

// 组件挂载时初始化OSS客户端
onMounted(() => {
  ossClient = initOssClient()
  if (!ossClient) {
    console.error('OSS客户端初始化失败，请检查环境变量配置')
  } else {
    console.log('OSS客户端初始化成功')
   
  }
})

// 是否可以提交
const canSubmit = computed(() => {
  return videoFile.value && videoTitle.value.trim() && (uploadProgress.value === 100 || uploadedVideoUrl.value)
})

// 处理视频文件选择
const handleVideoChange = (file) => {
  videoFile.value = file.raw
  // 创建预览URL
  if (file.raw) {
    videoPreviewUrl.value = URL.createObjectURL(file.raw)
  }
}

// 处理视频文件移除
const handleVideoRemove = () => {
  videoFile.value = null
  if (videoPreviewUrl.value) {
    URL.revokeObjectURL(videoPreviewUrl.value)
    videoPreviewUrl.value = ''
  }
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadedVideoUrl.value = ''
}

// 上传视频到OSS（使用分片上传）
const uploadVideoToOss = async () => {
  if (!videoFile.value) {
    throw new Error('请先选择视频文件')
  }

  if (!ossClient) {
    throw new Error('OSS客户端未初始化，请检查环境变量配置')
  }

  try {
    // 生成唯一的对象名称
    const fileName = videoFile.value.name
    const fileExtension = fileName.substring(fileName.lastIndexOf('.'))
    const objectName = `videos/${Date.now()}_${Math.random().toString(36).substring(2)}${fileExtension}`

    // 重置进度
    uploadProgress.value = 0
    uploadStatus.value = ''

    // 使用分片上传（multipartUpload）
    // 根据文档，对于大文件（>100MB）建议使用分片上传
    const result = await ossClient.multipartUpload(objectName, videoFile.value, {
      // 进度回调函数，p是0-1之间的小数
      progress: (p, checkpoint) => {
        uploadProgress.value = Math.round(p * 100)
        // checkpoint可以用于断点续传
        // console.log('上传进度:', p, '检查点:', checkpoint)
      },
      // 分片大小，默认1MB，可以根据需要调整
      // partSize: 1024 * 1024 * 5, // 5MB per part
      // 并发上传的分片个数，默认5
      // parallel: 5,
      // 设置headers
      headers: {
        'x-oss-object-acl': 'public-read' // 设置为公共读
      }
    })

    uploadProgress.value = 100
    uploadStatus.value = 'success'

    // 返回视频URL
    // multipartUpload返回的result结构：{ name, url, res }
    let videoUrl = result.url || (result.res && result.res.requestUrls && result.res.requestUrls[0])
    if (!videoUrl) {
      // 如果result中没有url，手动构建URL
      const bucket = import.meta.env.VITE_OSS_BUCKET || 'zhangrui123'
      const region = import.meta.env.VITE_OSS_REGION || 'cn-beijing'
      videoUrl = `https://${bucket}.${region}.aliyuncs.com/${objectName}`
    }
    uploadedVideoUrl.value = videoUrl
    return videoUrl
  } catch (error) {
    console.error('上传视频失败:', error)
    uploadStatus.value = 'exception'
    uploadProgress.value = 0
    
    // 处理超时错误
    if (error.code === 'ConnectionTimeoutError') {
      throw new Error('上传超时，请检查网络连接后重试')
    }
    
    // 处理ETag错误（分片上传必需）
    if (error.message && error.message.includes('etag of expose-headers')) {
      throw new Error('分片上传失败：请在OSS控制台的"跨域设置(CORS)"中，将"ETag"添加到"暴露Headers（ExposeHeader）"字段中。这是分片上传的必需配置。')
    }
    
    // 处理CORS错误
    if (error.message && (error.message.includes('CORS') || error.message.includes('Access-Control-Allow-Origin'))) {
      throw new Error('CORS跨域错误：请在阿里云OSS控制台的"跨域设置(CORS)"中配置规则，允许来源：http://localhost:5173，允许方法：GET, POST, PUT, DELETE, HEAD, OPTIONS，暴露Headers：ETag, x-oss-request-id')
    }
    
    // 处理网络错误（可能是CORS导致的）
    if (error.code === 'RequestError' || error.message?.includes('blocked by CORS policy')) {
      throw new Error('请求被CORS策略阻止：请在OSS控制台配置跨域规则，允许来源：http://localhost:5173')
    }
    
    // 处理认证错误
    if (error.code === 'InvalidAccessKeyId' || error.code === 'SignatureDoesNotMatch') {
      throw new Error('OSS认证失败，请检查AccessKey配置')
    }
    
    throw new Error(error.message || '上传失败，请重试')
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  if (!videoFile.value) {
    ElMessage.warning('请先选择视频文件')
    return
  }

  savingDraft.value = true
  try {
    // 上传视频到OSS
    const videoUrl = await uploadVideoToOss()

    // 保存草稿到后端
    const res = await saveDraft({
      title: videoTitle.value || '未命名视频',
      videoUrl: videoUrl
    })

    if (res.code === 200) {
      draftId.value = res.data?.id
      ElMessage.success('草稿保存成功')
    } else {
      ElMessage.error(res.msg || '保存草稿失败')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error(error.message || '保存草稿失败')
  } finally {
    savingDraft.value = false
  }
}

// 提交视频
const handleSubmit = async () => {
  if (!videoFile.value) {
    ElMessage.warning('请先选择视频文件')
    return
  }

  if (!videoTitle.value.trim()) {
    ElMessage.warning('请输入视频标题')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要提交视频吗？',
      '确认提交',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    submitting.value = true

    // 如果还没有上传，先上传
    let videoUrl = ''
    if (uploadedVideoUrl.value) {
      // 如果已经上传过，使用已上传的URL
      videoUrl = uploadedVideoUrl.value
    } else if (draftId.value) {
      // 如果有草稿，尝试从草稿中获取URL
      const draftRes = await getDraftDetail(draftId.value)
      if (draftRes.code === 200 && draftRes.data?.videoUrl) {
        videoUrl = draftRes.data.videoUrl
      } else {
        videoUrl = await uploadVideoToOss()
      }
    } else {
      videoUrl = await uploadVideoToOss()
    }

    // 提交视频
    const res = await submitVideo({
      draftId: draftId.value,
      title: videoTitle.value.trim(),
      videoUrl: videoUrl
    })

    if (res.code === 200) {
      ElMessage.success('视频提交成功！')
      // 跳转到首页
      setTimeout(() => {
        router.push('/home')
      }, 1500)
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交视频失败:', error)
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  router.back()
}
</script>

<style scoped>
.upload-container {
  min-height: 100vh;
  background-color: #f4f4f5;
  padding: 24px;
}

.upload-content {
  max-width: 1000px;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.upload-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.upload-section {
  margin-bottom: 32px;
}

.section-label {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.video-uploader {
  width: 100%;
}

.video-preview {
  margin-top: 16px;
  text-align: center;
}

.upload-progress {
  margin-top: 16px;
}

.progress-text {
  text-align: center;
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

.upload-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

:deep(.el-upload) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
}
</style>


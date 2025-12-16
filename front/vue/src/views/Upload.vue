<template>
  <div class="upload-container">
    <div class="upload-content">
      <h2 class="upload-title">投稿</h2>
      
      <!-- 视频上传区域 -->
      <div class="upload-section">
        <div class="section-label">视频文件 <span style="color: red;">*</span></div>
        <el-upload
          ref="uploadRef"
          class="video-uploader"
          :auto-upload="false"
          :on-change="handleVideoChange"
          :on-remove="handleVideoRemove"
          :limit="1"
          accept="video/mp4"
          :disabled="videoUploaded"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            <span v-if="videoUploaded">视频已上传</span>
            <span v-else>将视频文件拖到此处，或<em>点击上传</em></span>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传mp4格式的视频文件
            </div>
          </template>
        </el-upload>
        
        <!-- 视频预览（仅显示已上传状态，不显示视频） -->
        <div v-if="videoUploaded && uploadedVideoUrl" class="video-info">
          <el-tag type="success" size="large">视频已上传</el-tag>
          <div class="video-name">{{ videoFileName || '已上传视频' }}</div>
        </div>
        <!-- 视频预览（仅在选择新文件时显示） -->
        <div v-else-if="videoPreviewUrl && videoFile" class="video-preview">
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

      <!-- 封面上传区域 -->
      <div class="upload-section">
        <div class="section-label">视频封面 <span style="color: red;">*</span></div>
        <el-upload
          ref="coverUploadRef"
          class="cover-uploader"
          :auto-upload="false"
          :on-change="handleCoverChange"
          :on-remove="handleCoverRemove"
          :limit="1"
          accept="image/*"
          :show-file-list="false"
          drag
        >
          <div v-if="coverPreviewUrl" class="cover-preview-container">
            <img :src="coverPreviewUrl" class="cover-preview" />
            <div class="cover-mask">
              <el-icon><edit /></el-icon>
              <span>点击更换封面</span>
            </div>
          </div>
          <template v-else>
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将封面图片拖到此处，或<em>点击上传</em>
            </div>
          </template>
          <template #tip>
            <div class="el-upload__tip">
              支持 JPG、PNG 等图片格式，建议尺寸 16:9
            </div>
          </template>
        </el-upload>
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

    <!-- 图片裁剪对话框 -->
    <el-dialog
      v-model="cropDialogVisible"
      title="裁剪封面图片（4:3比例）"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="crop-container">
        <div class="crop-wrapper">
          <canvas ref="cropCanvas" class="crop-canvas"></canvas>
          <div 
            ref="cropBox" 
            class="crop-box"
            :style="cropBoxStyle"
            @mousedown="startCrop"
          >
            <div class="crop-handle crop-handle-nw"></div>
            <div class="crop-handle crop-handle-ne"></div>
            <div class="crop-handle crop-handle-sw"></div>
            <div class="crop-handle crop-handle-se"></div>
          </div>
        </div>
        <div class="crop-preview">
          <div class="preview-label">预览（4:3）</div>
          <canvas ref="previewCanvas" class="preview-canvas"></canvas>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelCrop">取消</el-button>
        <el-button type="primary" @click="confirmCrop">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Edit } from '@element-plus/icons-vue'
import { saveDraft, submitVideo, getDraftDetail, getDraftList } from '../api/uploadApi.js'
import OSS from 'ali-oss'

const router = useRouter()

// 上传组件引用
const uploadRef = ref(null)
const coverUploadRef = ref(null)

// 视频文件
const videoFile = ref(null)
const videoPreviewUrl = ref('')
const videoUploaded = ref(false) // 视频是否已上传
const videoFileName = ref('') // 视频文件名（用于显示）

// 封面文件
const coverFile = ref(null)
const coverPreviewUrl = ref('')
const uploadedCoverUrl = ref('') // 已上传的封面URL

// 裁剪相关
const cropDialogVisible = ref(false)
const cropCanvas = ref(null)
const previewCanvas = ref(null)
const cropBox = ref(null)
const originalImage = ref(null)
const cropImage = ref(null) // 裁剪后的图片文件
const cropBoxStyle = ref({})
const cropData = ref({
  x: 0,
  y: 0,
  width: 0,
  height: 0,
  scale: 1
})
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })

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

// 组件挂载时初始化OSS客户端并加载草稿
onMounted(async () => {
  ossClient = initOssClient()
  if (!ossClient) {
    console.error('OSS客户端初始化失败，请检查环境变量配置')
  } else {
    console.log('OSS客户端初始化成功')
  }
  
  // 加载草稿数据
  await loadDraft()
})

// 是否可以提交
const canSubmit = computed(() => {
  // 视频：要么有文件，要么已上传（从草稿恢复的情况）
  const hasVideo = videoFile.value || uploadedVideoUrl.value
  // 封面：必须已上传
  const hasCover = uploadedCoverUrl.value
  // 标题：必须填写
  const hasTitle = videoTitle.value.trim()
  
  return hasVideo && hasTitle && hasCover
})

// 处理视频文件选择
const handleVideoChange = (file) => {
  videoFile.value = file.raw
  videoFileName.value = file.name
  // 创建预览URL
  if (file.raw) {
    videoPreviewUrl.value = URL.createObjectURL(file.raw)
  }
  videoUploaded.value = false
}

// 处理视频文件移除
const handleVideoRemove = () => {
  videoFile.value = null
  videoFileName.value = ''
  if (videoPreviewUrl.value) {
    URL.revokeObjectURL(videoPreviewUrl.value)
    videoPreviewUrl.value = ''
  }
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadedVideoUrl.value = ''
  videoUploaded.value = false
}

// 处理封面文件选择
const handleCoverChange = async (file) => {
  coverFile.value = file.raw
  if (file.raw) {
    // 打开裁剪对话框
    await openCropDialog(file.raw)
  }
}

// 打开裁剪对话框
const openCropDialog = async (file) => {
  const img = new Image()
  img.onload = async () => {
    originalImage.value = img
    cropDialogVisible.value = true
    
    await nextTick()
    initCropCanvas(img)
  }
  img.src = URL.createObjectURL(file)
}

// 初始化裁剪画布
const initCropCanvas = (img) => {
  const canvas = cropCanvas.value
  const preview = previewCanvas.value
  if (!canvas || !preview) return

  // 设置画布尺寸
  const maxWidth = 700
  const maxHeight = 500
  let canvasWidth = img.width
  let canvasHeight = img.height
  const scale = Math.min(maxWidth / canvasWidth, maxHeight / canvasHeight, 1)
  
  canvasWidth = canvasWidth * scale
  canvasHeight = canvasHeight * scale
  
  canvas.width = canvasWidth
  canvas.height = canvasHeight
  canvas.style.width = canvasWidth + 'px'
  canvas.style.height = canvasHeight + 'px'
  
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, 0, 0, canvasWidth, canvasHeight)
  
  // 初始化裁剪框（4:3比例）
  const cropWidth = Math.min(canvasWidth * 0.8, canvasHeight * 0.8 * (4/3))
  const cropHeight = cropWidth / (4/3)
  const cropX = (canvasWidth - cropWidth) / 2
  const cropY = (canvasHeight - cropHeight) / 2
  
  cropData.value = {
    x: cropX,
    y: cropY,
    width: cropWidth,
    height: cropHeight,
    scale: scale
  }
  
  updateCropBox()
  updatePreview()
}

// 更新裁剪框样式
const updateCropBox = () => {
  cropBoxStyle.value = {
    left: cropData.value.x + 'px',
    top: cropData.value.y + 'px',
    width: cropData.value.width + 'px',
    height: cropData.value.height + 'px'
  }
}

// 更新预览
const updatePreview = () => {
  const canvas = cropCanvas.value
  const preview = previewCanvas.value
  if (!canvas || !preview) return

  const previewSize = 200
  preview.width = previewSize
  preview.height = previewSize * (3/4)
  
  const ctx = preview.getContext('2d')
  const sourceX = cropData.value.x / cropData.value.scale
  const sourceY = cropData.value.y / cropData.value.scale
  const sourceWidth = cropData.value.width / cropData.value.scale
  const sourceHeight = cropData.value.height / cropData.value.scale
  
  ctx.drawImage(
    originalImage.value,
    sourceX, sourceY, sourceWidth, sourceHeight,
    0, 0, preview.width, preview.height
  )
}

// 开始拖拽裁剪框
const startCrop = (e) => {
  if (e.target.classList.contains('crop-handle')) {
    // 调整大小（暂时不实现，只实现移动）
    return
  }
  
  isDragging.value = true
  dragStart.value = {
    x: e.clientX - cropData.value.x,
    y: e.clientY - cropData.value.y
  }
  
  document.addEventListener('mousemove', onCropMove)
  document.addEventListener('mouseup', stopCrop)
  e.preventDefault()
}

// 拖拽移动裁剪框
const onCropMove = (e) => {
  if (!isDragging.value) return
  
  const canvas = cropCanvas.value
  if (!canvas) return
  
  const newX = e.clientX - dragStart.value.x
  const newY = e.clientY - dragStart.value.y
  
  // 限制在画布范围内
  cropData.value.x = Math.max(0, Math.min(newX, canvas.width - cropData.value.width))
  cropData.value.y = Math.max(0, Math.min(newY, canvas.height - cropData.value.height))
  
  updateCropBox()
  updatePreview()
}

// 停止拖拽
const stopCrop = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onCropMove)
  document.removeEventListener('mouseup', stopCrop)
}

// 确认裁剪
const confirmCrop = () => {
  const canvas = cropCanvas.value
  if (!canvas || !originalImage.value) return

  // 创建裁剪后的canvas
  const croppedCanvas = document.createElement('canvas')
  const sourceX = cropData.value.x / cropData.value.scale
  const sourceY = cropData.value.y / cropData.value.scale
  const sourceWidth = cropData.value.width / cropData.value.scale
  const sourceHeight = cropData.value.height / cropData.value.scale
  
  croppedCanvas.width = sourceWidth
  croppedCanvas.height = sourceHeight
  
  const ctx = croppedCanvas.getContext('2d')
  ctx.drawImage(
    originalImage.value,
    sourceX, sourceY, sourceWidth, sourceHeight,
    0, 0, sourceWidth, sourceHeight
  )
  
  // 转换为Blob
  croppedCanvas.toBlob((blob) => {
    if (blob) {
      cropImage.value = new File([blob], 'cover.jpg', { type: 'image/jpeg' })
      coverFile.value = cropImage.value
      coverPreviewUrl.value = URL.createObjectURL(blob)
      cropDialogVisible.value = false
      
      // 清理
      if (originalImage.value.src.startsWith('blob:')) {
        URL.revokeObjectURL(originalImage.value.src)
      }
    }
  }, 'image/jpeg', 0.9)
}

// 取消裁剪
const cancelCrop = () => {
  cropDialogVisible.value = false
  coverFile.value = null
  if (originalImage.value && originalImage.value.src.startsWith('blob:')) {
    URL.revokeObjectURL(originalImage.value.src)
  }
  originalImage.value = null
}

// 处理封面文件移除
const handleCoverRemove = () => {
  coverFile.value = null
  if (coverPreviewUrl.value) {
    URL.revokeObjectURL(coverPreviewUrl.value)
    coverPreviewUrl.value = ''
  }
  uploadedCoverUrl.value = ''
}

// 上传封面到OSS（延迟上传，只在保存草稿或提交时调用）
const uploadCoverToOss = async () => {
  if (!coverFile.value) {
    throw new Error('请先选择封面文件')
  }

  if (!ossClient) {
    throw new Error('OSS客户端未初始化，请检查环境变量配置')
  }

  try {
    // 生成唯一的对象名称
    const objectName = `covers/${Date.now()}_${Math.random().toString(36).substring(2)}.jpg`

    // 上传封面（图片文件较小，使用普通上传）
    const result = await ossClient.put(objectName, coverFile.value, {
      headers: {
        'x-oss-object-acl': 'public-read' // 设置为公共读
      }
    })

    // 获取封面URL
    let coverUrl = result.url
    if (!coverUrl) {
      // 如果result中没有url，手动构建URL
      const bucket = import.meta.env.VITE_OSS_BUCKET || 'zhangrui123'
      const region = import.meta.env.VITE_OSS_REGION || 'cn-beijing'
      coverUrl = `https://${bucket}.${region}.aliyuncs.com/${objectName}`
    }
    
    uploadedCoverUrl.value = coverUrl
    return coverUrl
  } catch (error) {
    console.error('上传封面失败:', error)
    
    // 处理各种错误
    if (error.code === 'ConnectionTimeoutError') {
      throw new Error('上传超时，请检查网络连接后重试')
    }
    
    if (error.message && (error.message.includes('CORS') || error.message.includes('Access-Control-Allow-Origin'))) {
      throw new Error('CORS跨域错误：请在阿里云OSS控制台的"跨域设置(CORS)"中配置规则')
    }
    
    if (error.code === 'RequestError' || error.message?.includes('blocked by CORS policy')) {
      throw new Error('请求被CORS策略阻止：请在OSS控制台配置跨域规则')
    }
    
    if (error.code === 'InvalidAccessKeyId' || error.code === 'SignatureDoesNotMatch') {
      throw new Error('OSS认证失败，请检查AccessKey配置')
    }
    
    throw new Error(error.message || '上传失败，请重试')
  }
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
    videoUploaded.value = true // 标记视频已上传

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

// 加载草稿数据
const loadDraft = async () => {
  try {
    const res = await getDraftList()
    if (res.code === 200 && res.data) {
      const draft = res.data
      draftId.value = draft.id
      
      // 恢复标题
      if (draft.title) {
        videoTitle.value = draft.title
      }
      
      // 恢复视频（只保存URL，不显示视频预览）
      if (draft.videoUrl) {
        uploadedVideoUrl.value = draft.videoUrl
        videoUploaded.value = true
        uploadProgress.value = 100
        uploadStatus.value = 'success'
        // 从URL中提取文件名（如果有）
        try {
          const urlParts = draft.videoUrl.split('/')
          const fileName = urlParts[urlParts.length - 1]
          videoFileName.value = fileName || '已上传视频'
        } catch (e) {
          videoFileName.value = '已上传视频'
        }
        // 不设置 videoPreviewUrl，这样就不会显示视频预览
      }
      
      // 恢复封面
      if (draft.coverUrl) {
        uploadedCoverUrl.value = draft.coverUrl
        coverPreviewUrl.value = draft.coverUrl
      }
      
      ElMessage.success('已加载草稿数据')
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
    // 不显示错误提示，因为可能用户没有草稿
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  // 检查视频：要么有文件，要么已上传
  if (!videoFile.value && !uploadedVideoUrl.value) {
    ElMessage.warning('请先选择或上传视频文件')
    return
  }

  if (!coverFile.value && !uploadedCoverUrl.value) {
    ElMessage.warning('请先选择视频封面')
    return
  }

  savingDraft.value = true
  try {
    // 如果视频还没上传，先上传视频
    let videoUrl = uploadedVideoUrl.value
    if (!videoUrl && videoFile.value) {
      videoUrl = await uploadVideoToOss()
    } else if (!videoUrl) {
      ElMessage.warning('视频未上传，请先上传视频')
      return
    }

    // 如果封面还没上传，先上传封面
    let coverUrl = uploadedCoverUrl.value
    if (!coverUrl && coverFile.value) {
      coverUrl = await uploadCoverToOss()
    } else if (!coverUrl) {
      ElMessage.warning('封面未上传，请先上传封面')
      return
    }

    // 保存草稿（后端自动判断是更新还是创建）
    const res = await saveDraft({
      title: videoTitle.value || '未命名视频',
      videoUrl: videoUrl,
      coverUrl: coverUrl
    })

    if (res.code === 200) {
      if (res.data?.id) {
        draftId.value = res.data.id
      }
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
  // 检查视频：要么有文件，要么已上传
  if (!videoFile.value && !uploadedVideoUrl.value) {
    ElMessage.warning('请先选择或上传视频文件')
    return
  }

  if (!videoTitle.value.trim()) {
    ElMessage.warning('请输入视频标题')
    return
  }

  if (!coverFile.value && !uploadedCoverUrl.value) {
    ElMessage.warning('请先选择视频封面')
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
      } else if (videoFile.value) {
        videoUrl = await uploadVideoToOss()
      } else {
        ElMessage.error('视频未上传，请先上传视频')
        return
      }
    } else if (videoFile.value) {
      videoUrl = await uploadVideoToOss()
    } else {
      ElMessage.error('视频未上传，请先上传视频')
      return
    }

    // 如果封面还没上传，先上传封面
    let coverUrl = uploadedCoverUrl.value
    if (!coverUrl && coverFile.value) {
      coverUrl = await uploadCoverToOss()
    } else if (!coverUrl) {
      ElMessage.error('封面未上传，请先上传封面')
      return
    }

    // 提交视频
    const res = await submitVideo({
      draftId: draftId.value,
      title: videoTitle.value.trim(),
      videoUrl: videoUrl,
      coverUrl: coverUrl
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

.video-info {
  margin-top: 16px;
  text-align: center;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.video-name {
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
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

.cover-uploader {
  width: 100%;
}

.cover-preview-container {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 6px;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.cover-preview-container:hover .cover-mask {
  opacity: 1;
}

.cover-mask .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.cover-mask span {
  font-size: 14px;
}

/* 裁剪对话框样式 */
.crop-container {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.crop-wrapper {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.crop-canvas {
  display: block;
  max-width: 100%;
}

.crop-box {
  position: absolute;
  border: 2px solid #409eff;
  background: rgba(64, 158, 255, 0.1);
  cursor: move;
  box-sizing: border-box;
}

.crop-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #409eff;
  border: 2px solid #fff;
  border-radius: 50%;
}

.crop-handle-nw {
  top: -5px;
  left: -5px;
  cursor: nw-resize;
}

.crop-handle-ne {
  top: -5px;
  right: -5px;
  cursor: ne-resize;
}

.crop-handle-sw {
  bottom: -5px;
  left: -5px;
  cursor: sw-resize;
}

.crop-handle-se {
  bottom: -5px;
  right: -5px;
  cursor: se-resize;
}

.crop-preview {
  flex-shrink: 0;
}

.preview-label {
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  text-align: center;
}

.preview-canvas {
  display: block;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
}
</style>


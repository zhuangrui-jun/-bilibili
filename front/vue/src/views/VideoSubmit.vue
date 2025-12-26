<template>
  <div class="submit-container">
    <div class="submit-content">
      <h2 class="submit-title">完善视频信息</h2>
      
      <!-- 视频信息展示 -->
      <div class="submit-section">
        <div class="section-label">视频信息</div>
        <div class="video-info-card">
          <div class="info-item">
            <span class="info-label">草稿ID：</span>
            <el-tag type="info" size="large">{{ draftId || '未知' }}</el-tag>
          </div>
          <div v-if="draftData && draftData.videoUrl" class="info-item">
            <span class="info-label">视频预览：</span>
            <div class="video-preview-container">
              <video 
                :src="draftData.videoUrl" 
                controls 
                class="video-preview"
                preload="metadata"
              ></video>
            </div>
          </div>
        </div>
      </div>

      <!-- 封面上传区域 -->
      <div class="submit-section">
        <div class="section-label">视频封面 <span style="color: red;">*</span></div>
        <!-- 已有封面时显示封面图片 -->
        <div v-if="coverPreviewUrl" class="cover-display-container">
          <div class="cover-image-wrapper">
            <img :src="coverPreviewUrl" class="cover-display-image" />
            <div class="cover-overlay" @click="triggerCoverUpload">
              <el-icon><edit /></el-icon>
              <span>点击更换封面</span>
            </div>
          </div>
          <el-upload
            ref="coverUploadRef"
            class="cover-uploader-hidden"
            :auto-upload="false"
            :on-change="handleCoverChange"
            :limit="1"
            accept="image/*"
            :show-file-list="false"
          >
            <template #trigger>
              <span style="display: none;"></span>
            </template>
          </el-upload>
        </div>
        <!-- 没有封面时显示上传组件 -->
        <el-upload
          v-else
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
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将封面图片拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 JPG、PNG 等图片格式，建议尺寸 4:3
            </div>
          </template>
        </el-upload>
      </div>

      <!-- 标题输入 -->
      <div class="submit-section">
        <div class="section-label">视频标题 <span style="color: red;">*</span></div>
        <el-input
          v-model="videoTitle"
          placeholder="请输入视频标题"
          maxlength="100"
          show-word-limit
          clearable
        />
      </div>

      <!-- 操作按钮 -->
      <div class="submit-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="!canSubmit">
          提交
        </el-button>
      </div>
    </div>

    <!-- 图片裁剪对话框 -->
    <el-dialog
      v-model="cropDialogVisible"
      title="裁剪封面图片"
      width="900px"
      :close-on-click-modal="false"
      class="crop-dialog"
    >
      <div class="crop-container">
        <div class="crop-main-area">
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
          <div class="crop-tips">
            <p>拖拽选择框调整裁剪区域（4:3比例）</p>
          </div>
        </div>
        <div class="crop-preview-area">
          <div class="preview-label">预览效果</div>
          <div class="preview-wrapper">
            <canvas ref="previewCanvas" class="preview-canvas"></canvas>
          </div>
          <div class="preview-info">4:3 比例</div>
        </div>
      </div>
      <template #footer>
        <el-button size="large" @click="cancelCrop">取消</el-button>
        <el-button type="primary" size="large" @click="confirmCrop">确定裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Edit } from '@element-plus/icons-vue'
import { submitVideo, getDraftList, saveDraft, deleteDraft } from '../api/uploadApi.js'
import OSS from 'ali-oss'

const router = useRouter()
const route = useRoute()

// 草稿ID
const draftId = ref(null)

// 上传组件引用
const coverUploadRef = ref(null)

// 封面文件
const coverFile = ref(null)
const coverPreviewUrl = ref('')
const uploadedCoverUrl = ref('')

// 裁剪相关
const cropDialogVisible = ref(false)
const cropCanvas = ref(null)
const previewCanvas = ref(null)
const cropBox = ref(null)
const originalImage = ref(null)
const cropImage = ref(null)
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

// 视频标题
const videoTitle = ref('')

// 草稿数据
const draftData = ref(null)

// 提交状态
const submitting = ref(false)

// 自动保存定时器
const autoSaveTimer = ref(null)

// OSS客户端
let ossClient = null

// 是否可以提交
const canSubmit = computed(() => {
  const hasCover = coverFile.value || coverPreviewUrl.value || uploadedCoverUrl.value
  const hasTitle = videoTitle.value.trim()
  return hasCover && hasTitle
})

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

  const endpoint = `${region}.aliyuncs.com`

  return new OSS({
    region: region,
    accessKeyId: accessKeyId,
    accessKeySecret: accessKeySecret,
    bucket: bucket,
    endpoint: endpoint,
    secure: true
  })
}

// 加载草稿数据
const loadDraft = async () => {
  const id = route.params.draftId
  if (id) {
    draftId.value = String(id)
  }

  try {
    // 使用 getDraftList 获取当前用户的草稿
    const res = await getDraftList()
    if (res.code === 200 && res.data) {
      draftData.value = res.data
      // 显示视频ID（后端返回的id可能是字符串，因为使用了@JsonSerialize）
      draftId.value = String(res.data.id || id || '未知')
      
      if (res.data.title && res.data.title !== '未命名视频') {
        videoTitle.value = res.data.title
      }
      if (res.data.coverUrl) {
        coverPreviewUrl.value = res.data.coverUrl
        uploadedCoverUrl.value = res.data.coverUrl
      }
      // 确保有视频URL
      if (!res.data.videoUrl) {
        ElMessage.warning('草稿中缺少视频信息，请返回重新上传')
        // 添加标记，避免循环跳转
        router.push({
          path: '/upload',
          query: { fromSubmit: 'true' }
        })
        return
      }
    } else {
      ElMessage.error(res.msg || '加载草稿失败')
      // 添加标记，避免循环跳转
      router.push({
        path: '/upload',
        query: { fromSubmit: 'true' }
      })
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '加载草稿失败')
    // 添加标记，避免循环跳转
    router.push({
      path: '/upload',
      query: { fromSubmit: 'true' }
    })
  }
}

// 组件挂载时初始化
onMounted(async () => {
  ossClient = initOssClient()
  if (!ossClient) {
    console.error('OSS客户端初始化失败，请检查环境变量配置')
  }
  
  await loadDraft()
})

// 触发封面上传
const triggerCoverUpload = () => {
  nextTick(() => {
    if (coverUploadRef.value) {
      if (coverUploadRef.value.clearFiles) {
        coverUploadRef.value.clearFiles()
      }
      
      const uploadEl = coverUploadRef.value.$el
      const input = uploadEl?.querySelector('input[type="file"]')
      if (input) {
        input.value = ''
        input.click()
      } else {
        const tempInput = document.createElement('input')
        tempInput.type = 'file'
        tempInput.accept = 'image/*'
        tempInput.style.display = 'none'
        tempInput.onchange = (e) => {
          const file = e.target.files?.[0]
          if (file) {
            handleCoverChange({ raw: file })
          }
          if (document.body.contains(tempInput)) {
            document.body.removeChild(tempInput)
          }
        }
        document.body.appendChild(tempInput)
        tempInput.click()
      }
    }
  })
}

// 处理封面文件选择
const handleCoverChange = async (file) => {
  if (!file || !file.raw) {
    return
  }
  
  coverFile.value = null
  cropImage.value = null
  
  coverFile.value = file.raw
  await openCropDialog(file.raw)
}

// 打开裁剪对话框
const openCropDialog = async (file) => {
  if (originalImage.value && originalImage.value.src.startsWith('blob:')) {
    URL.revokeObjectURL(originalImage.value.src)
  }
  
  const img = new Image()
  const blobUrl = URL.createObjectURL(file)
  
  img.onload = async () => {
    originalImage.value = img
    cropDialogVisible.value = true
    
    await nextTick()
    initCropCanvas(img)
  }
  
  img.onerror = () => {
    URL.revokeObjectURL(blobUrl)
    ElMessage.error('图片加载失败，请重试')
  }
  
  img.src = blobUrl
}

// 初始化裁剪画布
const initCropCanvas = (img) => {
  const canvas = cropCanvas.value
  const preview = previewCanvas.value
  if (!canvas || !preview) return

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
    return
  }
  
  const canvas = cropCanvas.value
  if (!canvas) return
  
  const rect = canvas.getBoundingClientRect()
  
  isDragging.value = true
  dragStart.value = {
    x: e.clientX - cropData.value.x - rect.left,
    y: e.clientY - cropData.value.y - rect.top
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
  
  const rect = canvas.getBoundingClientRect()
  
  const newX = e.clientX - dragStart.value.x - rect.left
  const newY = e.clientY - dragStart.value.y - rect.top
  
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
  
  croppedCanvas.toBlob((blob) => {
    if (blob) {
      if (coverPreviewUrl.value && coverPreviewUrl.value.startsWith('blob:')) {
        URL.revokeObjectURL(coverPreviewUrl.value)
      }
      
      uploadedCoverUrl.value = ''
      
      cropImage.value = new File([blob], 'cover.jpg', { type: 'image/jpeg' })
      coverFile.value = cropImage.value
      coverPreviewUrl.value = URL.createObjectURL(blob)
      cropDialogVisible.value = false
      
      if (originalImage.value.src.startsWith('blob:')) {
        URL.revokeObjectURL(originalImage.value.src)
      }
      originalImage.value = null
      
      nextTick(() => {
        if (coverUploadRef.value && coverUploadRef.value.clearFiles) {
          coverUploadRef.value.clearFiles()
        }
      })
    }
  }, 'image/jpeg', 0.9)
}

// 取消裁剪
const cancelCrop = () => {
  cropDialogVisible.value = false
  
  coverFile.value = null
  cropImage.value = null
  
  if (originalImage.value && originalImage.value.src.startsWith('blob:')) {
    URL.revokeObjectURL(originalImage.value.src)
  }
  originalImage.value = null
  
  if (!uploadedCoverUrl.value && coverPreviewUrl.value && coverPreviewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(coverPreviewUrl.value)
    coverPreviewUrl.value = ''
  }
  
  nextTick(() => {
    if (coverUploadRef.value && coverUploadRef.value.clearFiles) {
      coverUploadRef.value.clearFiles()
    }
  })
}

// 处理封面文件移除
const handleCoverRemove = () => {
  coverFile.value = null
  cropImage.value = null
  if (coverPreviewUrl.value) {
    if (coverPreviewUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(coverPreviewUrl.value)
    }
    coverPreviewUrl.value = ''
  }
  uploadedCoverUrl.value = ''
}

// 上传封面到OSS
const uploadCoverToOss = async () => {
  const fileToUpload = cropImage.value || coverFile.value
  if (!fileToUpload) {
    throw new Error('请先选择封面文件')
  }

  if (!ossClient) {
    throw new Error('OSS客户端未初始化，请检查环境变量配置')
  }

  try {
    const objectName = `covers/${Date.now()}_${Math.random().toString(36).substring(2)}.jpg`

    const result = await ossClient.put(objectName, fileToUpload, {
      headers: {
        'x-oss-object-acl': 'public-read'
      }
    })

    let coverUrl = result.url
    if (!coverUrl) {
      const bucket = import.meta.env.VITE_OSS_BUCKET || 'zhangrui123'
      const region = import.meta.env.VITE_OSS_REGION || 'cn-beijing'
      coverUrl = `https://${bucket}.${region}.aliyuncs.com/${objectName}`
    }
    
    try {
      const urlObj = new URL(coverUrl)
      coverUrl = `${urlObj.protocol}//${urlObj.host}${urlObj.pathname}`
    } catch (e) {
      const queryIndex = coverUrl.indexOf('?')
      if (queryIndex !== -1) {
        coverUrl = coverUrl.substring(0, queryIndex)
      }
    }
    
      uploadedCoverUrl.value = coverUrl
      
      // 自动保存草稿（更新封面URL）
      if (draftId.value) {
        try {
          await saveDraft({
            title: videoTitle.value.trim() || '未命名视频',
            videoUrl: draftData.value?.videoUrl || '',
            coverUrl: coverUrl
          })
        } catch (error) {
          console.error('自动保存草稿失败:', error)
          // 不显示错误提示，因为这只是自动保存
        }
      }
      
      return coverUrl
  } catch (error) {
    console.error('上传封面失败:', error)
    throw new Error(error.message || '上传失败，请重试')
  }
}

// 提交视频
const handleSubmit = async () => {
  if (!videoTitle.value.trim()) {
    ElMessage.warning('请输入视频标题')
    return
  }

  if (!coverFile.value && !coverPreviewUrl.value && !uploadedCoverUrl.value) {
    ElMessage.warning('请先选择视频封面')
    return
  }

  if (!draftData.value || !draftData.value.videoUrl) {
    ElMessage.error('视频信息缺失，请返回重新上传')
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

    let coverUrl = uploadedCoverUrl.value
    if (!coverUrl && (coverFile.value || coverPreviewUrl.value)) {
      coverUrl = await uploadCoverToOss()
    } else if (!coverUrl) {
      ElMessage.error('封面未上传，请先上传封面')
      return
    }

    // draftId 保持为字符串，后端会自动转换为 Long 类型
    // 注意：不要使用 Number() 转换，会导致大整数精度丢失
    const draftIdStr = draftId.value && draftId.value !== '未知' ? String(draftId.value) : null
    
    const res = await submitVideo({
      draftId: draftIdStr,
      title: videoTitle.value.trim(),
      videoUrl: draftData.value.videoUrl,
      coverUrl: coverUrl
    })

    if (res.code === 200) {
      ElMessage.success('视频提交成功！')
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

// 自动保存草稿
const autoSaveDraft = async () => {
  if (!draftId.value || !draftData.value) {
    return
  }
  
  // 防抖：避免频繁保存
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  autoSaveTimer.value = setTimeout(async () => {
    try {
      await saveDraft({
        title: videoTitle.value.trim() || '未命名视频',
        videoUrl: draftData.value.videoUrl || '',
        coverUrl: uploadedCoverUrl.value || ''
      })
      // 不显示成功提示，避免打扰用户
    } catch (error) {
      console.error('自动保存草稿失败:', error)
      // 不显示错误提示，因为这只是自动保存
    }
  }, 1000) // 1秒后保存
}

// 从OSS URL中提取objectKey
const extractObjectKeyFromUrl = (url) => {
  if (!url) return null
  
  try {
    const urlObj = new URL(url)
    // 提取路径部分，去掉开头的斜杠
    let objectKey = urlObj.pathname
    if (objectKey.startsWith('/')) {
      objectKey = objectKey.substring(1)
    }
    return objectKey
  } catch (e) {
    // 如果不是标准URL格式，尝试从字符串中提取
    // 格式可能是：https://bucket.region.aliyuncs.com/path/to/file
    const match = url.match(/https?:\/\/[^\/]+\/(.+)/)
    if (match && match[1]) {
      return match[1].split('?')[0] // 去掉查询参数
    }
    return null
  }
}

// 删除OSS文件
const deleteOssFile = async (url) => {
  if (!url || !ossClient) {
    return
  }
  
  try {
    const objectKey = extractObjectKeyFromUrl(url)
    if (!objectKey) {
      console.warn('无法从URL中提取objectKey:', url)
      return
    }
    
    await ossClient.delete(objectKey)
    console.log('OSS文件删除成功:', objectKey)
  } catch (error) {
    console.error('删除OSS文件失败:', error)
    // 不抛出错误，避免影响删除草稿流程
    // 如果文件不存在或已删除，OSS会返回404，这是可以接受的
  }
}

// 删除草稿（包括OSS文件）
const deleteDraftWithOss = async () => {
  // 先获取草稿信息，以便删除OSS文件
  let videoUrl = null
  let coverUrl = null
  
  try {
    const res = await getDraftList()
    if (res.code === 200 && res.data) {
      videoUrl = res.data.videoUrl
      coverUrl = res.data.coverUrl
    }
  } catch (error) {
    console.error('获取草稿信息失败:', error)
    // 继续执行删除，即使获取信息失败
  }
  
  // 删除OSS中的视频文件
  if (videoUrl) {
    await deleteOssFile(videoUrl)
  }
  
  // 删除OSS中的封面文件
  if (coverUrl) {
    await deleteOssFile(coverUrl)
  }
  
  // 删除数据库中的草稿记录
  await deleteDraft()
}

// 取消
const handleCancel = async () => {
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  try {
    // 使用 ElMessageBox 提供两个选项
    await ElMessageBox({
      title: '退出确认',
      message: '请选择退出方式',
      distinguishCancelAndClose: true,
      showCancelButton: true,
      confirmButtonText: '保存草稿退出',
      cancelButtonText: '退出而不保存草稿',
      type: 'info',
      beforeClose: async (action, instance, done) => {
        if (action === 'confirm') {
          // 保存草稿退出
          instance.confirmButtonLoading = true
          try {
            let coverUrl = uploadedCoverUrl.value
            // 如果封面还没上传，先上传
            if (!coverUrl && (coverFile.value || coverPreviewUrl.value)) {
              try {
                coverUrl = await uploadCoverToOss()
              } catch (error) {
                console.error('上传封面失败:', error)
                // 如果上传失败，使用空字符串
                coverUrl = ''
              }
            }
            
            await saveDraft({
              title: videoTitle.value.trim() || '未命名视频',
              videoUrl: draftData.value?.videoUrl || '',
              coverUrl: coverUrl || ''
            })
            
            ElMessage.success('草稿已保存')
            done()
            router.push('/home')
          } catch (error) {
            console.error('保存草稿失败:', error)
            ElMessage.error('保存草稿失败，请重试')
            instance.confirmButtonLoading = false
          }
        } else if (action === 'cancel') {
          // 退出而不保存草稿（删除草稿）
          instance.cancelButtonLoading = true
          try {
            await deleteDraftWithOss()
            ElMessage.success('草稿已删除')
            done()
            router.push('/home')
          } catch (error) {
            console.error('删除草稿失败:', error)
            ElMessage.error('删除草稿失败，请重试')
            instance.cancelButtonLoading = false
          }
        } else {
          // 关闭对话框（点击 X 或按 ESC）
          done()
        }
      }
    })
  } catch (error) {
    // 用户点击了关闭按钮或按了 ESC，直接退出
    if (error !== 'cancel' && error !== 'close') {
      console.error('取消操作失败:', error)
    }
    // 直接退出，不保存也不删除
    router.push('/home')
  }
}
</script>

<style scoped>
.submit-container {
  min-height: 100vh;
  background-color: #f4f4f5;
  padding: 24px;
}

.submit-content {
  max-width: 1000px;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.submit-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.submit-section {
  margin-bottom: 32px;
}

.section-label {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.cover-uploader {
  width: 100%;
}

.cover-uploader-hidden {
  display: none;
}

.cover-display-container {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
}

.cover-image-wrapper {
  position: relative;
  width: 400px;
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.cover-image-wrapper:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.cover-display-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

.cover-overlay {
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

.cover-image-wrapper:hover .cover-overlay {
  opacity: 1;
}

.cover-overlay .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.cover-overlay span {
  font-size: 14px;
}

.submit-actions {
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

/* 裁剪对话框样式 */
:deep(.crop-dialog) {
  .el-dialog__body {
    padding: 24px;
  }
}

.crop-container {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.crop-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.crop-wrapper {
  position: relative;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.crop-canvas {
  display: block;
  max-width: 100%;
  background: #fff;
}

.crop-box {
  position: absolute;
  border: 3px solid #409eff;
  background: rgba(64, 158, 255, 0.15);
  cursor: move;
  box-sizing: border-box;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
}

.crop-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #409eff;
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.crop-handle-nw {
  top: -6px;
  left: -6px;
  cursor: nw-resize;
}

.crop-handle-ne {
  top: -6px;
  right: -6px;
  cursor: ne-resize;
}

.crop-handle-sw {
  bottom: -6px;
  left: -6px;
  cursor: sw-resize;
}

.crop-handle-se {
  bottom: -6px;
  right: -6px;
  cursor: se-resize;
}

.crop-tips {
  margin-top: 16px;
  text-align: center;
}

.crop-tips p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.crop-preview-area {
  flex-shrink: 0;
  width: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-label {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  text-align: center;
}

.preview-wrapper {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px;
  background: #f5f7fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.preview-canvas {
  display: block;
  border-radius: 4px;
  background: #fff;
}

.preview-info {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

/* 视频信息卡片样式 */
.video-info-card {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-item {
  margin-bottom: 16px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-right: 8px;
}

.video-preview-container {
  margin-top: 12px;
  width: 100%;
  max-width: 600px;
}

.video-preview {
  width: 100%;
  max-height: 400px;
  border-radius: 6px;
  background-color: #000;
}
</style>


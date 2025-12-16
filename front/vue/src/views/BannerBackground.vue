<template>
  <div
    ref="bannerContainer"
    class="banner-container"
    @mouseenter="handleMouseEnter"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <div
      v-for="(item, index) in processedData"
      :key="index"
      class="layer"
      :style="getLayerStyle(item, index)"
    >
      <img
        v-if="item.tagName === 'img'"
        :src="item.src"
        :style="getImageStyle(index)"
        draggable="false"
      />
      <video
        v-else-if="item.tagName === 'video'"
        :src="item.src"
        :style="getImageStyle(index)"
        loop
        autoplay
        muted
        playsinline
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import bannerData from '../data/bannerData.js'

const props = defineProps({
  bannerIndex: {
    type: Number,
    default: 0
  }
})

// 响应式数据
const bannerContainer = ref(null)
const compensate = ref(1) // 视窗补偿值
const initX = ref(0) // 鼠标初始X位置
const moveX = ref(0) // 鼠标移动距离
const startTime = ref(0) // 动画开始时间
const isHoming = ref(false) // 是否正在回正
const homingProgress = ref(0) // 回正动画进度
const animationFrameId = ref(null)

// 当前使用的数据
const currentData = computed(() => {
  return bannerData[props.bannerIndex]?.data || bannerData[0]?.data || []
})

// 处理后的数据（应用补偿值）
const processedData = computed(() => {
  const cloneData = JSON.parse(JSON.stringify(currentData.value))
  return cloneData.map(item => {
    const processedItem = { ...item }
    if (compensate.value !== 1) {
      processedItem.transform[4] = item.transform[4] * compensate.value
      processedItem.transform[5] = item.transform[5] * compensate.value
    }
    return processedItem
  })
})

// 线性插值函数
const lerp = (start, end, amt) => (1 - amt) * start + amt * end

// 计算补偿值
const calculateCompensate = () => {
  compensate.value = window.innerWidth > 1650 ? window.innerWidth / 1650 : 1
}

// 获取图层样式
const getLayerStyle = (item, index) => {
  const itemData = currentData.value[index]
  if (!itemData) return {}
  
  let m = new DOMMatrix(itemData.transform)
  let move = moveX.value * (itemData.a || 0) // 移动X translateX
  let s = itemData.f ? itemData.f * moveX.value + 1 : 1 // 放大比例 Scale
  let g = moveX.value * (itemData.g || 0) // 移动Y translateY
  
  if (isHoming.value) {
    // 回正时处理
    const progress = homingProgress.value
    const currentMoveX = moveX.value
    m.e = lerp(
      currentMoveX * (itemData.a || 0) + itemData.transform[4],
      itemData.transform[4],
      progress
    )
    move = 0
    s = lerp(itemData.f ? itemData.f * currentMoveX + 1 : 1, 1, progress)
    g = lerp(itemData.g ? itemData.g * currentMoveX : 0, 0, progress)
  }
  
  // 应用缩放和移动
  m = m.multiply(new DOMMatrix([m.a * s, m.b, m.c, m.d * s, move, g]))
  
  // 应用旋转
  if (itemData.deg) {
    const deg = isHoming.value
      ? lerp(itemData.deg * moveX.value, 0, homingProgress.value)
      : itemData.deg * moveX.value
    m = m.multiply(
      new DOMMatrix([
        Math.cos(deg),
        Math.sin(deg),
        -Math.sin(deg),
        Math.cos(deg),
        0,
        0,
      ])
    )
  }
  
  // 计算透明度
  let opacity = itemData.opacity?.[0] || 1
  if (itemData.opacity && itemData.opacity.length === 2) {
    if (isHoming.value && moveX.value > 0) {
      opacity = lerp(itemData.opacity[1], itemData.opacity[0], homingProgress.value)
    } else {
      opacity = lerp(
        itemData.opacity[0],
        itemData.opacity[1],
        (moveX.value / window.innerWidth) * 2
      )
    }
  }
  
  return {
    transform: m.toString(),
    opacity: opacity
  }
}

// 获取图片/视频样式
const getImageStyle = (index) => {
  const itemData = currentData.value[index]
  if (!itemData) return {}
  
  return {
    width: `${itemData.width * compensate.value}px`,
    height: `${itemData.height * compensate.value}px`,
    filter: `blur(${itemData.blur || 0}px)`,
    userSelect: 'none',
    pointerEvents: 'none'
  }
}

// 鼠标进入
const handleMouseEnter = (e) => {
  initX.value = e.pageX
  if (isHoming.value) {
    // 如果正在回正，取消回正动画
    if (animationFrameId.value) {
      cancelAnimationFrame(animationFrameId.value)
    }
    isHoming.value = false
    homingProgress.value = 0
    startTime.value = 0
  }
}

// 鼠标移动
const handleMouseMove = (e) => {
  if (isHoming.value) return
  moveX.value = e.pageX - initX.value
}

// 鼠标离开
const handleMouseLeave = () => {
  if (!isHoming.value) {
    isHoming.value = true
    startTime.value = 0 // 重置，让 homing 函数使用 requestAnimationFrame 的 timestamp
    homingProgress.value = 0
    requestAnimationFrame(homing)
  }
}

// 回正动画
const homing = (timestamp) => {
  if (!isHoming.value) return
  
  if (!startTime.value) {
    startTime.value = timestamp
  }
  
  const elapsed = timestamp - startTime.value
  const progress = Math.min(elapsed / 300, 1)
  homingProgress.value = progress
  
  if (progress < 1) {
    animationFrameId.value = requestAnimationFrame(homing)
  } else {
    isHoming.value = false
    moveX.value = 0
    homingProgress.value = 0
    startTime.value = 0
  }
}

// 窗口大小改变
const handleResize = () => {
  calculateCompensate()
}

// 窗口失焦
const handleBlur = () => {
  handleMouseLeave()
}

// 初始化
onMounted(() => {
  calculateCompensate()
  window.addEventListener('resize', handleResize)
  window.addEventListener('blur', handleBlur)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('blur', handleBlur)
  if (animationFrameId.value) {
    cancelAnimationFrame(animationFrameId.value)
  }
})

// 监听数据变化，重新计算补偿值
watch(() => props.bannerIndex, () => {
  calculateCompensate()
})
</script>

<style scoped>
.banner-container {
  position: relative;
  overflow: hidden;
  margin: 0 auto;
  min-width: 1000px;
  min-height: 155px;
  height: 10vw;
  max-height: 240px;
}

.layer {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

img,
video {
  user-select: none;
  pointer-events: none;
}
</style>

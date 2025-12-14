<template>
  <div class="banner-background-container">
    <div id="app" class="parallax-bg" ref="parallaxContainer">
      <div v-if="loading" class="loading-text">loading...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { bannerImagesData } from '../data/bannerData.js'

// 容器引用
const parallaxContainer = ref(null)

// 状态
const loading = ref(true)
let allImagesData = bannerImagesData
let compensate = 0 // 视窗补偿值
let layers = [] // DOM集合
let initX = 0
let moveX = 0
let startTime = 0
const duration = 300 // 动画持续时间（毫秒）
let animationFrameId = null

// 线性插值函数
const lerp = (start, end, amt) => (1 - amt) * start + amt * end

// 添加图片元素
function initItems() {
  if (!parallaxContainer.value) return
  
  compensate = window.innerWidth > 1650 ? window.innerWidth / 1650 : 1
  
  if (layers.length <= 0) {
    parallaxContainer.value.style.display = 'none'
    
    for (let i = 0; i < allImagesData.length; i++) {
      const item = allImagesData[i]
      
      // 跳过没有URL的项或blob URL
      if (!item.url || item.url.startsWith('blob:')) continue
      
      const layer = document.createElement('div')
      layer.classList.add('layer')
      layer.style.transform = new DOMMatrix(item.transform).toString()
      
      if (item.opacity) {
        layer.style.opacity = item.opacity[0]
      }
      
      const img = document.createElement('img')
      img.src = item.url
      img.style.filter = `blur(${item.blur || 0}px)`
      img.style.width = `${item.width * compensate}px`
      
      layer.appendChild(img)
      parallaxContainer.value.appendChild(layer)
    }
    
    parallaxContainer.value.style.display = ''
    layers = parallaxContainer.value.querySelectorAll('.layer')
  } else {
    for (let i = 0; i < layers.length; i++) {
      if (layers[i].firstElementChild && allImagesData[i]) {
        layers[i].firstElementChild.style.width = `${allImagesData[i].width * compensate}px`
      }
    }
  }
}

// 鼠标移动处理
function mouseMove() {
  animate()
}

// 鼠标离开处理
function leave() {
  startTime = 0
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  animationFrameId = requestAnimationFrame(homing)
}

// 回正动画
function homing(timestamp) {
  if (!startTime) {
    startTime = timestamp
  }
  
  const elapsed = timestamp - startTime
  const progress = Math.min(elapsed / duration, 1)
  
  animate(progress)
  
  if (progress < 1) {
    animationFrameId = requestAnimationFrame(homing)
  } else {
    animationFrameId = null
  }
}

// 动画执行
function animate(progress) {
  if (layers.length <= 0) return
  
  const isHoming = typeof progress === 'number'
  
  for (let i = 0; i < layers.length; i++) {
    const layer = layers[i]
    const item = allImagesData[i]
    
    if (!item) continue
    
    let m = new DOMMatrix(item.transform)
    let move = moveX * item.a // 移动X translateX
    let s = item.f ? item.f * moveX + 1 : 1 // 放大比例 Scale
    let g = moveX * (item.g || 0) // 移动Y translateY
    
    if (isHoming) { // 回正时处理
      m.e = lerp(moveX * item.a + item.transform[4], item.transform[4], progress)
      move = 0
      s = lerp(item.f ? item.f * moveX + 1 : 1, 1, progress)
      g = lerp(item.g ? item.g * moveX : 0, 0, progress)
    }
    
    m = m.multiply(new DOMMatrix([m.a * s, m.b, m.c, m.d * s, move, g]))
    
    if (item.deg) { // 有旋转角度
      const deg = isHoming ? lerp(item.deg * moveX, 0, progress) : item.deg * moveX
      m = m.multiply(new DOMMatrix([
        Math.cos(deg), Math.sin(deg),
        -Math.sin(deg), Math.cos(deg),
        0, 0
      ]))
    }
    
    if (item.opacity) { // 有透明度变化
      layer.style.opacity = isHoming && moveX > 0
        ? lerp(item.opacity[1], item.opacity[0], progress)
        : lerp(item.opacity[0], item.opacity[1], moveX / window.innerWidth * 2)
    }
    
    layer.style.transform = m.toString()
  }
}

// 设置事件监听
const setupEventListeners = () => {
  if (!parallaxContainer.value) return
  
  // 鼠标滑入与滑动
  parallaxContainer.value.addEventListener('mouseover', (e) => {
    initX = e.pageX
  })
  
  parallaxContainer.value.addEventListener('mousemove', (e) => {
    moveX = e.pageX - initX
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
    }
    animationFrameId = requestAnimationFrame(mouseMove)
  })
  
  // 鼠标已经离开了视窗或者切出浏览器，执行回正动画
  parallaxContainer.value.addEventListener('mouseleave', leave)
  window.addEventListener('blur', leave)
  
  // 添加窗口大小监听
  window.addEventListener('resize', initItems)
}

// 清理事件监听
const cleanupEventListeners = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  window.removeEventListener('blur', leave)
  window.removeEventListener('resize', initItems)
}

// 组件挂载时初始化
onMounted(async () => {
  await nextTick()
  initItems()
  setupEventListeners()
  loading.value = false
})

// 组件卸载时清理
onBeforeUnmount(() => {
  cleanupEventListeners()
})
</script>

<style scoped>
.banner-background-container {
  width: 100%;
  position: relative;
}

#app {
  position: relative;
  overflow: hidden;
  margin: 0 auto;
  min-width: 1000px;
  min-height: 155px;
  height: 10vw;
  max-height: 240px;
  width: 100%;
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

.layer img {
  user-select: none;
  pointer-events: none;
}

.loading-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  z-index: 10;
}
</style>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="title">欢迎使用</h1>
        <p class="subtitle">使用邮箱登录或注册账号</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="email">
              <el-input
                v-model="loginForm.email"
                placeholder="请输入邮箱"
                size="large"
                :prefix-icon="Message"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="login-form"
            @submit.prevent="handleRegister"
          >
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                size="large"
                :prefix-icon="Message"
                clearable
              />
            </el-form-item>
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（至少6位）"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-input-wrapper">
                <el-input
                  v-model="registerForm.code"
                  placeholder="请输入验证码"
                  size="large"
                  :prefix-icon="Key"
                  clearable
                  class="code-input"
                />
                <el-button
                  type="primary"
                  size="large"
                  :disabled="codeCountdown > 0 || !isEmailValid"
                  :loading="codeLoading"
                  @click="handleSendCode"
                  class="code-btn"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}秒后重试` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Message, Lock, User, Key } from '@element-plus/icons-vue'
import { sendCode, register, login } from '../api/userApi.js'
import { useRouter } from 'vue-router'

const router = useRouter()

// 当前激活的tab
const activeTab = ref('login')

// 登录表单
const loginForm = ref({
  email: '',
  password: ''
})

// 注册表单
const registerForm = ref({
  email: '',
  username: '',
  password: '',
  confirmPassword: '',
  code: ''
})

// 表单引用
const loginFormRef = ref(null)
const registerFormRef = ref(null)

// 加载状态
const loginLoading = ref(false)
const registerLoading = ref(false)
const codeLoading = ref(false)

// 验证码倒计时
const codeCountdown = ref(0)
let countdownTimer = null

// 邮箱格式验证
const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

// 验证邮箱格式是否有效
const isEmailValid = computed(() => {
  return emailRegex.test(registerForm.value.email)
})

// 登录表单验证规则
const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: emailRegex, message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 注册表单验证规则
const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: emailRegex, message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名长度至少2位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// Tab切换处理
const handleTabChange = (tabName) => {
  // 重置表单
  if (tabName === 'login') {
    loginFormRef.value?.resetFields()
  } else {
    registerFormRef.value?.resetFields()
    // 清除倒计时
    if (countdownTimer) {
      clearInterval(countdownTimer)
      codeCountdown.value = 0
    }
  }
}

// 发送验证码
const handleSendCode = async () => {
  if (!isEmailValid.value) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  codeLoading.value = true
  try {
    const res = await sendCode(registerForm.value.email)
    if (res.code === 200) {
      ElMessage.success(res.data || '验证码已发送，有效时长5分钟')
      // 开始倒计时（60秒）
      codeCountdown.value = 60
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      ElMessage.error(res.msg || '发送验证 码失败')
    }
  } catch (error) {
    // 网络错误或其他异常，已在request拦截器中处理
    console.error('发送验证码失败:', error)
  } finally {
    codeLoading.value = false
  }
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loginLoading.value = true
    try {
      const res = await login(loginForm.value.email, loginForm.value.password)
      console.log("res:",res)
      if (res.code === 200 && res.data) {
        ElMessage.success('登录成功')
        // 保存用户信息和token
        localStorage.setItem('user', JSON.stringify(res.data))
        localStorage.setItem('token', res.data.token)
        // 跳转到首页或其他页面
        router.push('/test')
      } else {
        ElMessage.error(res.msg || '登录失败')
      }
    } catch (error) {
      // 错误已在request拦截器中处理，这里不需要再次显示
      console.error('登录失败:', error)
    } finally {
      loginLoading.value = false
    }
  })
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    registerLoading.value = true
    try {
      const res = await register(
        registerForm.value.email,
        registerForm.value.username,
        registerForm.value.password,
        registerForm.value.code
      )
      if (res.code === 200) {
        ElMessage.success(res.data || '注册成功，请返回登录')
        // 切换到登录tab
        activeTab.value = 'login'
        // 自动填充邮箱
        loginForm.value.email = registerForm.value.email
        // 重置注册表单
        registerFormRef.value.resetFields()
      } else {
        ElMessage.error(res.msg || '注册失败')
      }
    } catch (error) {
      // 错误已在request拦截器中处理，这里不需要再次显示
      console.error('注册失败:', error)
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.login-tabs {
  margin-top: 24px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  padding: 0 24px;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #667eea;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #667eea;
}

.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.login-form :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
}

.login-form :deep(.el-input__wrapper) {
  background-color: #ffffff;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
}

.code-input-wrapper {
  display: flex;
  gap: 12px;
}

.code-input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
  border-radius: 8px;
  min-width: 120px;
}

.submit-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-box {
    padding: 32px 24px;
  }

  .title {
    font-size: 24px;
  }

  .code-input-wrapper {
    flex-direction: column;
  }

  .code-btn {
    width: 100%;
  }
}
</style>


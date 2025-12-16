import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    fs: {
      // 允许访问项目根目录外的文件
      allow: ['..']
    }
  },
  // 配置静态资源别名
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  // 配置 public 目录，使其可以访问上级目录的资源
  publicDir: 'public'
})

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      // 替换 index.html 中的环境变量
      {
        name: 'html-transform',
        transformIndexHtml(html) {
          return html
            .replace(/%VITE_AMAP_KEY%/g, env.VITE_AMAP_KEY || '')
            .replace(/%VITE_AMAP_SECURITY_CODE%/g, env.VITE_AMAP_SECURITY_CODE || '')
        }
      }
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})

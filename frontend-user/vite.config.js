import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  // 检查关键环境变量
  if (!env.VITE_AMAP_KEY && mode === 'production') {
    console.warn('[WARNING] VITE_AMAP_KEY 未设置，地图功能将不可用。请在 .env 文件中配置高德地图 API Key。')
  }

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
      host: '0.0.0.0',
      proxy: {
        '/api': {
          // Docker 环境下使用服务名，本地开发使用 localhost
          target: process.env.VITE_API_TARGET || (process.env.DOCKER_ENV ? 'http://backend:8080' : 'http://localhost:8080'),
          changeOrigin: true,
          // 不重写路径，后端 context path 就是 /api
          rewrite: (path) => path
        },
        '/ws': {
          target: process.env.VITE_WS_TARGET || (process.env.DOCKER_ENV ? 'ws://backend:8080' : 'ws://localhost:8080'),
          ws: true,
          changeOrigin: true,
          // 重写：从 /ws/* -> /ws/*（保持路径不变，后端在 /ws/chat 和 /ws/pile-status）
          rewrite: (path) => path
        }
      }
    }
  }
})

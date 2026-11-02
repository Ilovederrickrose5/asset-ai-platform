import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发环境统一经 API 网关（9000）转发，与生产入口保持一致
// 网关按路径分发：/api/auth|users|roles|departments → asset-auth
//                  /api/ai → asset-ai
//                  其余 /api/** → asset-business
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          // SSE 长连接需要关闭压缩缓冲
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept-Encoding', 'identity')
          })
          proxy.on('error', (err) => {
            console.error('Gateway proxy error', err.message)
          })
        }
      }
    }
  }
})

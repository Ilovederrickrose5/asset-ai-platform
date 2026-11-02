import axios from 'axios'
import { ElMessage } from 'element-plus'
import { pinia } from '../stores'
import { useUserStore } from '../stores/userStore'

const useStore = () => useUserStore(pinia)

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 后端API基础URL
  timeout: 10000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从 store 获取 token（store 与 localStorage 同步）
    const token = useStore().token
    // 如果token存在，添加到请求头
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    // 处理请求错误
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 直接返回响应数据
    return response
  },
  error => {
    // 处理响应错误
    // 401错误处理（未认证）
    if (error.response && error.response.status === 401) {
      // 检查是否是登录请求失败（通过config.url判断）
      // 登录请求失败时不应该跳转页面，否则会导致登录页刷新
      const isLoginRequest = error.config && error.config.url && error.config.url.includes('/auth/login')

      if (!isLoginRequest) {
        // 非登录请求的401错误（token过期），统一通过 store 登出（清空 store + localStorage）
        useStore().logout()
        // 跳转到登录页面
        window.location.href = '/'
      }
      // 登录请求失败时，不跳转页面，让组件自行处理错误显示
    }
    // 403错误处理（无权限）
    if (error.response && error.response.status === 403) {
      const message = error.response.data?.message || '没有权限执行该操作'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service

import { createPinia } from 'pinia'

// 全局 pinia 实例：供组件外（router / utils / request）使用
export const pinia = createPinia()

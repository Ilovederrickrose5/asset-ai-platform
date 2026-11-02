import { useUserStore } from '../stores/userStore'

// 权限指令：角色不匹配时把元素从 DOM 移除（不是 v-show）
// 用法：v-permission="'ADMIN'" 或 v-permission="['ADMIN', 'MANAGER']"
export const vPermission = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const userRole = userStore.currentUserRole

    let requiredRoles = binding.value
    if (!requiredRoles) return

    if (!Array.isArray(requiredRoles)) {
      requiredRoles = [requiredRoles]
    }

    // 统一转为小写比较，兼容大小写差异
    const normalizedUserRole = String(userRole).toLowerCase()
    const normalizedRequired = requiredRoles.map(r => String(r).toLowerCase())

    if (!normalizedRequired.includes(normalizedUserRole)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export default vPermission

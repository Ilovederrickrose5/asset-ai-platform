// 权限控制工具函数（角色来源统一为 userStore）
import { menuConfig } from '../config/menuConfig'
import { pinia } from '../stores'
import { useUserStore } from '../stores/userStore'

const useStore = () => useUserStore(pinia)

// 获取用户的真实角色（统一从 store 取，store 内部兼容 roles 数组 / role 字段 / 用户名）
export function getCurrentUserRole() {
  return useStore().currentUserRole
}

// 检查用户是否有权限访问路由
export function hasPermission(route, userRole) {
  if (!route.meta || !route.meta.roles) {
    return true // 没有配置角色权限的路由默认允许访问
  }
  return route.meta.roles.includes(userRole)
}

// 检查用户是否有权限执行操作（角色层级比较）
export function hasOperationPermission(requiredRole, userRole) {
  const roleHierarchy = {
    admin: 4,
    leader: 3,
    manager: 2,
    user: 1,
  }
  return (roleHierarchy[userRole] || 0) >= (roleHierarchy[requiredRole] || 0)
}

// 检查用户是否为管理员
export function hasAdminPermission() {
  return useStore().currentUserRole === 'admin'
}

// 获取用户可访问的菜单
export function getAccessibleMenus() {
  const userRole = useStore().currentUserRole

  // 深拷贝菜单数组，避免修改原始数据
  const filteredMenus = JSON.parse(JSON.stringify(menuConfig))

  return filteredMenus.filter((menu) => {
    // 检查父菜单权限
    if (menu.roles && !menu.roles.includes(userRole)) {
      return false
    }

    // 如果有子菜单，也需要过滤子菜单
    if (menu.children && menu.children.length > 0) {
      menu.children = menu.children.filter((child) => {
        if (!child.roles) return true

        // 检查子菜单权限
        if (!child.roles.includes(userRole)) {
          return false
        }

        // 根据角色设置不同的菜单标题
        if (child.titleByRole && child.titleByRole[userRole]) {
          child.title = child.titleByRole[userRole]
        }

        return true
      })
      // 如果过滤后没有子菜单了，且父菜单本身没有权限配置，则不显示父菜单
      if (menu.children.length === 0 && !menu.roles) {
        return false
      }
    }

    return true
  })
}

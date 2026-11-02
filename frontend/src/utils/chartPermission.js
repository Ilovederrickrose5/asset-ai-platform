// 图表权限控制工具（角色来源统一为 userStore）
import { pinia } from '../stores'
import { useUserStore } from '../stores/userStore'

const useStore = () => useUserStore(pinia)

// 检查用户是否有权限访问图表
export function checkChartPermission(chartType) {
  const userRole = useStore().currentUserRole

  switch (chartType) {
    case 'status':
      return checkStatusChartPermission(userRole)
    case 'department':
      return checkDepartmentChartPermission(userRole)
    default:
      return false
  }
}

// 检查用户是否有修改权限
export function checkModifyPermission() {
  // 只有系统管理员有修改权限
  return useStore().currentUserRole === 'admin'
}

// 检查资产状态分布图表权限
function checkStatusChartPermission(userRole) {
  const rolePermissions = {
    admin: true,
    leader: true,
    manager: true,
    user: false,
  }
  return rolePermissions[userRole] || false
}

// 检查部门资产统计图表权限
function checkDepartmentChartPermission(userRole) {
  const rolePermissions = {
    admin: true,
    leader: true,
    manager: true,
    user: false,
  }
  return rolePermissions[userRole] || false
}

// 获取用户访问级别（与 userStore 的角色优先级保持一致）
export function getAccessLevel() {
  switch (useStore().currentUserRole) {
    case 'admin':
      return 4 // 完全访问
    case 'leader':
      return 3 // 本部门数据访问
    case 'manager':
      return 2 // 本部门资产维护
    case 'user':
      return 1 // 仅个人数据
    default:
      return 1
  }
}

// 检查是否有权限访问特定部门的数据
export function hasDepartmentAccess(departmentId) {
  const store = useStore()
  const userRole = store.currentUserRole

  if (userRole === 'admin') {
    return true
  }

  if (userRole === 'leader' || userRole === 'manager') {
    return store.departmentId != null && Number(store.departmentId) === Number(departmentId)
  }

  return false
}

// 用户数据字段映射函数 - 确保前后端字段名一致
export const normalizeUser = (user) => {
  if (!user) return null
  
  return {
    id: user.id,
    username: user.username,
    realName: user.realName,
    email: user.email,
    // 统一部门ID字段名 - 优先使用 departmentId，兼容 deptId
    departmentId: user.departmentId || user.deptId,
    departmentName: user.departmentName,
    // 角色字段统一为数组格式
    roles: Array.isArray(user.roles) ? user.roles : (user.role ? [user.role] : []),
    phone: user.phone,
    position: user.position,
    employeeNo: user.employeeNo,
    status: user.status
  }
}

// 获取状态文本
export const getStatusText = (status, statusMap = {
  pending: '待审批',
  approved: '已批准',
  rejected: '已拒绝',
  in_progress: '维修中',
  completed: '已完成'
}) => {
  return statusMap[status] || status
}

// 获取状态标签类型
export const getStatusType = (status, typeMap = {
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  in_progress: 'info',
  completed: 'success'
}) => {
  return typeMap[status] || 'info'
}

// 时间格式化
export const formatTime = (time, fallback = '-') => {
  if (!time) return fallback
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 获取资产信息
export const getAssetInfo = (assetId, assets) => {
  if (!assets || !Array.isArray(assets)) return '未知资产'
  const asset = assets.find(a => a.id === assetId)
  return asset ? `${asset.assetName} - ${asset.model || ''}` : '未知资产'
}

// 获取资产名称
export const getAssetName = (assetId, assets) => {
  if (!assets || !Array.isArray(assets)) return '未知资产'
  const asset = assets.find(a => a.id === assetId)
  return asset ? asset.assetName : '未知资产'
}

// 获取资产型号
export const getAssetModel = (assetId, assets) => {
  if (!assets || !Array.isArray(assets)) return ''
  const asset = assets.find(a => a.id === assetId)
  return asset ? asset.model : ''
}
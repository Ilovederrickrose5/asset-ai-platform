// 菜单配置
// 角色说明：
// - admin: 系统管理员（所有权限）
// - leader: 领导（系统概览、资产管理（除资产申请）、资产统计）
// - manager: 部门资产管理员（系统概览、资产管理（除资产申请）、除资产统计）
// - user: 普通员工（系统概览、资产列表、资产申请）

export const menuConfig = [
  {
    id: 1,
    title: '系统概览',
    icon: '📊',
    path: '/dashboard',
    roles: ['admin', 'leader', 'manager', 'user']
  },
  {
    id: 8,
    title: 'AI 知识助手',
    icon: '💬',
    path: '/ai-chat',
    roles: ['admin', 'leader', 'manager', 'user']
  },
  {
    id: 2,
    title: '资产管理',
    icon: '🏢',
    // 父菜单不设置roles，根据子菜单是否有权限来决定是否显示
    children: [
      {
        id: 21,
        title: '我的资产',
        titleByRole: {  // 根据角色显示不同的菜单名称
          'admin': '资产列表',
          'leader': '部门资产',
          'manager': '部门资产',
          'user': '我的资产'
        },
        path: '/assets/list',
        roles: ['admin', 'leader', 'manager', 'user'],
        icon: '📦'
      },
      {
        id: 22,
        title: '资产领用申请',
        path: '/assets/apply',
        roles: ['admin', 'user'],  // 只有系统管理员和普通员工可以提交申请
        icon: '📋'
      },
      {
        id: 28,
        title: '资产转移申请',
        path: '/assets/transfer',
        roles: ['admin', 'user'],  // 只有系统管理员和普通员工可以提交申请
        icon: '↔️'
      },
      {
        id: 29,
        title: '资产报废申请',
        path: '/assets/disposal',
        roles: ['admin', 'user'],  // 只有系统管理员和普通员工可以提交申请
        icon: '🗑️'
      },
      {
        id: 32,
        title: '资产维修申请',
        path: '/assets/apply?type=MAINTENANCE',
        roles: ['admin', 'user'],  // 只有系统管理员和普通员工可以提交申请
        icon: '📋'
      },
      {
        id: 34,
        title: '申请记录',
        path: '/my-applications',
        roles: ['admin', 'leader', 'manager', 'user'],  // 所有角色都可以查看申请记录（根据角色权限过滤）
        icon: '📝'
      },
      {
        id: 30,
        title: '资产统计',
        path: '/reports',
        roles: ['leader', 'admin', 'manager'],  // 领导、管理员和系统管理员可以看统计
        icon: '📈'
      },
      {
        id: 31,
        title: '折旧计算',
        path: '/depreciation',
        roles: ['leader', 'admin', 'manager'],  // 领导、管理员和系统管理员可以进行折旧计算
        icon: '💰'
      },
      {
        id: 38,
        title: '资产盘点',
        titleByRole: {  // 根据角色显示不同的菜单名称
          'admin': '资产盘点',
          'leader': '资产盘点',
          'manager': '资产盘点',
          'user': '我的盘点任务'
        },
        path: '/asset-inventory',
        roles: ['leader', 'admin', 'manager', 'user'],  // 所有角色都可以访问
        icon: '✅'
      }
    ]
  },
  {
    id: 5,
    title: '审批管理',
    icon: '✅',
    // 父菜单不设置roles，根据子菜单是否有权限来决定是否显示
    children: [
      {
        id: 23,
        title: '资产领用审批',
        path: '/assets/approve?type=RECEIVE',
        roles: ['admin', 'manager']  // 只有管理员和系统管理员可以审批
      },
      {
        id: 24,
        title: '资产转移审批',
        path: '/assets/approve?type=TRANSFER',
        roles: ['admin', 'manager']  // 只有管理员和系统管理员可以审批
      },
      {
        id: 25,
        title: '资产维修审批',
        path: '/assets/approve?type=MAINTENANCE',
        roles: ['admin', 'manager']  // 只有管理员和系统管理员可以审批
      },
      {
        id: 26,
        title: '资产报废审批',
        path: '/assets/approve?type=DISPOSAL',
        roles: ['admin', 'leader', 'manager']  // 领导、管理员、系统管理员可以审批报废
      }
    ]
  },
  {
    id: 3,
    title: '基础数据管理',
    icon: '📁',
    // 父菜单不设置roles，根据子菜单是否有权限来决定是否显示
    children: [
      {
        id: 35,
        title: '部门管理',
        path: '/departments',
        roles: ['admin']  // 只有系统管理员
      },
      {
        id: 36,
        title: '资产分类管理',
        path: '/asset-categories',
        roles: ['admin']  // 只有系统管理员
      },
      {
        id: 37,
        title: '员工信息管理',
        path: '/users',
        roles: ['admin']  // 只有系统管理员
      },
      {
        id: 39,
        title: '供应商管理',
        path: '/suppliers',
        roles: ['admin']  // 只有系统管理员
      }
    ]
  },
  {
    id: 7,
    title: '采购管理',
    icon: '🛒',
    // 父菜单不设置roles，根据子菜单是否有权限来决定是否显示
    children: [
      {
        id: 71,
        title: '采购需求申请',
        path: '/purchase/request',
        roles: ['admin', 'manager']  // 系统管理员、部门资产管理员
      },
      {
        id: 72,
        title: '采购审批',
        path: '/purchase/approval',
        roles: ['admin', 'leader']  // 系统管理员、领导
      },
      {
        id: 73,
        title: '采购订单',
        path: '/purchase/orders',
        roles: ['admin', 'manager']  // 系统管理员、部门资产管理员
      }
    ]
  },

]

// 快捷操作按钮配置
export const quickActions = [
  {
    id: 1,
    title: '资产领用申请',
    icon: '📋',
    path: '/assets/apply',
    roles: ['user']  // 只有普通员工
  },
  {
    id: 2,
    title: '资产报表',
    icon: '📊',
    path: '/reports',
    roles: ['leader', 'admin', 'manager']  // 领导、管理员和系统管理员
  },
  {
    id: 3,
    title: '资产查询',
    icon: '🔍',
    path: '/assets/list',
    roles: ['admin', 'leader', 'manager', 'user']  // 所有角色
  },

]

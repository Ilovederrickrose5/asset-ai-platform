import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Department from '../views/Department.vue'
import AssetCategory from '../views/AssetCategory.vue'
import User from '../views/User.vue'
import AssetList from '../views/AssetList.vue'
import AssetApply from '../views/AssetApply.vue'
import AssetApproval from '../views/AssetApproval.vue'

import AssetTransfer from '../views/AssetTransfer.vue'
import AssetDisposal from '../views/AssetDisposal.vue'
import Report from '../views/Report.vue'
import PurchaseRequest from '../views/PurchaseRequest.vue'
import PurchaseApproval from '../views/PurchaseApproval.vue'

import MyApplications from '../views/MyApplications.vue'
import Supplier from '../views/Supplier.vue'
import { getCurrentUserRole, hasPermission } from '../utils/permission'
import { useUserStore } from '../stores/userStore'

// 错误页面
const NotFound = () => import('../views/NotFound.vue')
const Forbidden = () => import('../views/Forbidden.vue')

const routes = [
  {
    path: '/',
    name: 'Login',
    component: Login
  },
  {
    path: '/login',
    redirect: '/'
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Home,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: () => import('../views/AiChat.vue'),
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/assets',
    redirect: '/assets/list'
  },
  {
    path: '/assets/list',
    name: 'AssetList',
    component: AssetList,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/assets/apply',
    name: 'AssetApply',
    component: AssetApply,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/assets/approve',
    name: 'AssetApprove',
    component: AssetApproval,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager']
    }
  },

  {
    path: '/assets/transfer',
    name: 'AssetTransfer',
    component: AssetTransfer,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/assets/disposal',
    name: 'AssetDisposal',
    component: AssetDisposal,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/assets/statistics',
    name: 'AssetStatistics',
    component: Home,
    meta: {
      requiresAuth: true,
      roles: ['leader', 'admin']
    }
  },
  {
    path: '/users',
    name: 'Users',
    component: User,
    meta: {
      requiresAuth: true,
      roles: ['admin']
    }
  },
  {
    path: '/departments',
    name: 'Departments',
    component: Department,
    meta: {
      requiresAuth: true,
      roles: ['admin']
    }
  },
  {
    path: '/asset-categories',
    name: 'AssetCategories',
    component: AssetCategory,
    meta: {
      requiresAuth: true,
      roles: ['admin']
    }
  },
  {
    path: '/suppliers',
    name: 'Suppliers',
    component: Supplier,
    meta: {
      requiresAuth: true,
      roles: ['admin']
    }
  },
  {
    path: '/reports',
    name: 'Report',
    component: Report,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager']
    }
  },
  {
    path: '/depreciation',
    name: 'Depreciation',
    component: () => import('../views/Depreciation.vue'),
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager']
    }
  },

  {
    path: '/my-applications',
    name: 'MyApplications',
    component: MyApplications,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader', 'manager', 'user']
    }
  },
  {
    path: '/asset-inventory',
    name: 'AssetInventory',
    component: () => import('../views/AssetInventory.vue'),
    meta: {
      requiresAuth: true,
      roles: ['leader', 'admin', 'manager', 'user']  // 所有角色都可以访问
    }
  },
  {
    path: '/purchase/request',
    name: 'PurchaseRequest',
    component: PurchaseRequest,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'manager']
    }
  },
  {
    path: '/purchase/approval',
    name: 'PurchaseApproval',
    component: PurchaseApproval,
    meta: {
      requiresAuth: true,
      roles: ['admin', 'leader']
    }
  },
  {
    path: '/purchase/orders',
    name: 'PurchaseOrder',
    component: () => import('../views/PurchaseOrder.vue'),
    meta: {
      requiresAuth: true,
      roles: ['admin', 'manager']
    }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: NotFound
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: Forbidden
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 检查是否需要认证
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      // 未登录，带上 redirect 参数跳登录页
      return { name: 'Login', query: { redirect: encodeURIComponent(to.fullPath) } }
    }

    // 检查角色权限（优先使用后端返回的角色信息）
    const userRole = getCurrentUserRole()

    if (!hasPermission(to, userRole)) {
      // 没有权限，跳 403
      return { name: 'Forbidden' }
    }
  }

  return true
})

export default router

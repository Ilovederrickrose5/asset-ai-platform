<template>
  <el-container class="home-container">
    <el-header class="header">
      <div class="header-content">
        <div class="header-left">
          <button
            class="nav-toggle"
            type="button"
            :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'"
            @click="isCollapse = !isCollapse"
          >
            <el-icon><Menu /></el-icon>
          </button>
          <div class="brand">
            <span class="brand-mark">EA</span>
            <div class="brand-copy">
              <strong>企业固定资产</strong>
              <span>Asset Management</span>
            </div>
          </div>
        </div>

        <div class="header-actions">
          <div class="header-search">
            <el-icon class="header-search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              class="header-search-input"
              type="text"
              placeholder="搜索资产名称或编号"
              @keyup.enter="handleSearch"
            />
            <button class="header-search-submit" type="button" @click="handleSearch">搜索</button>
          </div>

          <el-dropdown trigger="click">
            <button class="user-chip" type="button">
              <span class="avatar">{{ getUserName().slice(0, 1) }}</span>
              <span class="user-meta">
                <strong>{{ getUserName() }}</strong>
                <span>{{ getRoleName() }}</span>
              </span>
              <el-icon class="user-caret"><Setting /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="navigateTo('/settings')">系统设置</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    
    <el-container class="content">
      <el-aside :width="isCollapse ? '84px' : '248px'" class="sidebar" :class="{ 'sidebar-collapsed': isCollapse }">
        <div class="sidebar-inner">
          <p v-if="!isCollapse" class="nav-caption">导航</p>

          <el-menu
            :default-active="currentMenu"
            class="el-menu-vertical"
            @select="handleMenuSelect"
            :collapse="isCollapse"
          >
            <template v-for="menu in accessibleMenus" :key="menu.id">
              <!-- 有子菜单的情况 -->
              <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="String(menu.id)">
                <template #title>
                  <el-icon><component :is="getMenuIcon(menu.id)" /></el-icon>
                  <span v-if="!isCollapse">{{ menu.title }}</span>
                </template>
                <el-menu-item
                  v-for="subMenu in menu.children"
                  :key="subMenu.id"
                  :index="String(subMenu.id)"
                >
                  <el-icon><component :is="getMenuIcon(subMenu.id)" /></el-icon>
                  <span v-if="!isCollapse">{{ getMenuTitle(subMenu) }}</span>
                </el-menu-item>
              </el-sub-menu>
              <!-- 没有子菜单的情况 -->
              <el-menu-item v-else :index="String(menu.id)">
                <el-icon><component :is="getMenuIcon(menu.id)" /></el-icon>
                <span v-if="!isCollapse">{{ menu.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>

          <div v-if="!isCollapse" class="sidebar-footer">
            <!-- 待处理任务 -->
            <el-card v-if="pendingTasks.length > 0" class="pending-tasks-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><Operation /></el-icon></span>
                    <div>
                      <strong>待我处理</strong>
                      <span>需要关注的任务</span>
                    </div>
                  </div>
                  <span class="task-count">{{ pendingTasks.length }}</span>
                </div>
              </template>

              <el-scrollbar max-height="210px">
                <el-timeline>
                  <el-timeline-item
                    v-for="(task, index) in pendingTasks"
                    :key="index"
                    :timestamp="task.timeText"
                    placement="top"
                  >
                    <div class="task-content">
                      <el-tag type="warning" size="small" class="task-type" effect="plain">{{ getTypeLabel(task.type) }}</el-tag>
                      <span>{{ task.content }}</span>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </el-scrollbar>
            </el-card>

            <!-- 最近动态 -->
            <el-card class="recent-activities-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><Clock /></el-icon></span>
                    <div>
                      <strong>最近动态</strong>
                      <span>系统操作记录</span>
                    </div>
                  </div>
                </div>
              </template>

              <el-scrollbar max-height="230px">
                <el-timeline v-if="recentActivities.length > 0">
                  <el-timeline-item
                    v-for="(activity, index) in recentActivities"
                    :key="index"
                    :timestamp="activity.timeText"
                    placement="top"
                  >
                    <div class="activity-content">
                      <el-tag :type="getActivityTagType(activity.type)" size="small" class="activity-type" effect="plain">{{ getTypeLabel(activity.type) }}</el-tag>
                      <span><strong>{{ activity.operator }}</strong>{{ activity.content }}</span>
                    </div>
                  </el-timeline-item>
                </el-timeline>
                <div v-else class="empty-state">
                  <span class="empty-state__icon"><el-icon><Clock /></el-icon></span>
                  <strong>暂无操作记录</strong>
                  <span>系统操作后会自动记录在这里</span>
                </div>
              </el-scrollbar>
            </el-card>
          </div>
        </div>
      </el-aside>

      <div
        class="sidebar-scrim"
        :class="{ 'is-visible': !isCollapse }"
        @click="isCollapse = true"
      ></div>

      <el-main class="main-content">
        <section class="overview-hero">
          <div class="overview-copy">
            <p class="eam-eyebrow">OVERVIEW · {{ userDepartment }}</p>
            <h2>{{ greeting }}，{{ getUserName() }}</h2>
            <p class="overview-sub">这是当前的资产概览，数据实时更新。</p>
          </div>
          <div class="overview-meta">
            <span class="live-dot"></span>
            <span>数据已同步</span>
          </div>
        </section>

        <section class="stats-grid">
          <template v-if="getUserRoleFromLocalStorage() === 'user'">
            <article class="stat-card">
              <span class="stat-icon"><el-icon><Box /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">资产数量</span>
                <strong class="stat-value">{{ myAssetCount }}</strong>
                <span class="stat-meta">当前归属资产</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><TrendCharts /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">使用中</span>
                <strong class="stat-value">{{ usingAssetCount }}</strong>
                <span class="stat-meta">正在使用</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><Clock /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">闲置</span>
                <strong class="stat-value">{{ idleAssetCount }}</strong>
                <span class="stat-meta">可随时领用</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><Operation /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">维修中</span>
                <strong class="stat-value">{{ maintenanceAssetCount }}</strong>
                <span class="stat-meta">处理中资产</span>
              </div>
            </article>
          </template>

          <template v-else>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><OfficeBuilding /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">{{ getUserRoleFromLocalStorage() === 'admin' ? '公司资产总数' : '部门资产总数' }}</span>
                <strong class="stat-value">{{ totalAssetCount }}</strong>
                <span class="stat-meta">全部状态合计</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><Clock /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">待处理申请</span>
                <strong class="stat-value">{{ myApplicationCount }}</strong>
                <span class="stat-meta">等待审批</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><TrendCharts /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">使用中</span>
                <strong class="stat-value">{{ usingAssetCount }}</strong>
                <span class="stat-meta">已分配资产</span>
              </div>
            </article>
            <article class="stat-card">
              <span class="stat-icon"><el-icon><Box /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">闲置</span>
                <strong class="stat-value">{{ idleAssetCount }}</strong>
                <span class="stat-meta">可调配资产</span>
              </div>
            </article>
            <article v-if="getUserRoleFromLocalStorage() === 'admin'" class="stat-card stat-card--accent">
              <span class="stat-icon"><el-icon><Document /></el-icon></span>
              <div class="stat-body">
                <span class="stat-label">资产总价值</span>
                <strong class="stat-value stat-value--money" :title="formatPrice(totalAssetValue)">{{ formatMoneyCompact(totalAssetValue) }}</strong>
                <span class="stat-meta">按原值合计</span>
              </div>
            </article>
          </template>
        </section>

        <!-- 最近申请 (仅普通员工) -->
        <el-card v-if="getUserRoleFromLocalStorage() === 'user'" shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head">
              <div class="panel-title">
                <span class="panel-icon"><el-icon><Clock /></el-icon></span>
                <div>
                  <strong>最近申请</strong>
                  <span>你最近提交的资产申请</span>
                </div>
              </div>
              <el-button type="primary" plain size="small" @click="navigateTo('/my-applications')">
                <span>查看全部</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="recentApplications.length > 0" class="recent-applications">
            <el-timeline>
              <el-timeline-item
                v-for="(app, index) in recentApplications"
                :key="index"
                :timestamp="formatTime(app.applicationDate)"
                placement="top"
              >
                <el-card shadow="never" class="application-card">
                  <div class="application-content">
                    <div class="application-title">
                      <span>{{ getApplicationTypeName(app.applicationType) }}</span>
                      <el-tag :type="getApplicationStatusType(app.status)">
                        <span>{{ getStatusIcon(app.status) }}</span> {{ getApplicationStatusName(app.status) }}
                      </el-tag>
                    </div>
                    <div class="application-details">
                      <div class="detail-item">
                        <el-icon><Box /></el-icon>
                        <span>{{ app.assetName || '未知资产' }}</span>
                        <span v-if="app.assetNo" class="asset-no">({{ app.assetNo }})</span>
                      </div>
                      <div v-if="app.approvalRemark" class="detail-item">
                        <el-icon><Message /></el-icon>
                        <span>审批意见: {{ app.approvalRemark }}</span>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="empty-state">
            <span class="empty-state__icon"><el-icon><Files /></el-icon></span>
            <strong>暂无申请记录</strong>
            <span>提交领用、维修等申请后可在这里查看进度</span>
          </div>
        </el-card>

        <!-- 图表区域 -->
        <el-row :gutter="20" class="chart-row" v-if="checkChartPermission('status')">
          <el-col :xs="24" :md="12">
            <el-card class="chart-card panel-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><PieChart /></el-icon></span>
                    <div>
                      <strong>资产状态分布</strong>
                      <span>按当前状态统计</span>
                    </div>
                  </div>
                </div>
              </template>
              <div class="chart-container chart-container--donut">
                <div ref="statusChartRef" class="chart"></div>
                <div class="donut-center">
                  <strong>{{ statusTotal }}</strong>
                  <span>资产总数</span>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-card class="chart-card panel-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><TrendCharts /></el-icon></span>
                    <div>
                      <strong>资产价值分布</strong>
                      <span>各状态资产价值对比 · 对数刻度</span>
                    </div>
                  </div>
                </div>
              </template>
              <div class="chart-container">
                <div ref="valueChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 部门资产统计和详细数据 - 左右布局 -->
        <el-row :gutter="20" class="chart-row" v-if="checkChartPermission('department')">
          <el-col :xs="24" :md="11">
            <el-card class="department-card panel-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><OfficeBuilding /></el-icon></span>
                    <div>
                      <strong>部门资产统计</strong>
                      <span>部门资产数量与价值</span>
                    </div>
                  </div>
                  <el-button type="primary" plain size="small" @click="navigateTo('/departments')">
                    <span>查看详情</span>
                    <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </template>
              <div class="chart-container">
                <div ref="departmentChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="13">
            <el-card class="department-detail-card panel-card" shadow="never">
              <template #header>
                <div class="panel-head">
                  <div class="panel-title">
                    <span class="panel-icon"><el-icon><Document /></el-icon></span>
                    <div>
                      <strong>部门资产明细</strong>
                      <span>各维度资产数据</span>
                    </div>
                  </div>
                  <el-button type="primary" plain size="small" @click="exportDepartmentStats">
                    <span>导出数据</span>
                    <el-icon><Download /></el-icon>
                  </el-button>
                </div>
              </template>
              <el-table :data="departmentStats" style="width: 100%" max-height="300">
                <el-table-column prop="departmentName" label="部门名称" min-width="84" />
                <el-table-column prop="assetCount" label="资产数量" width="66" align="right" />
                <el-table-column prop="totalValue" label="资产总价值" width="96" align="right">
                  <template #default="scope">
                    <span :title="formatPrice(scope.row.totalValue)">{{ formatMoneyCompact(scope.row.totalValue) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="averageValue" label="平均价值" width="92" align="right">
                  <template #default="scope">
                    <span :title="formatPrice(scope.row.averageValue)">{{ formatMoneyCompact(scope.row.averageValue) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="inUseCount" label="使用中" width="58" align="right" />
                <el-table-column prop="idleCount" label="闲置" width="50" align="right" />
                <el-table-column prop="maintenanceCount" label="维修中" width="58" align="right" />
                <el-table-column prop="scrappedCount" label="已报废" width="58" align="right" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import * as echarts from 'echarts'
import { ElMessageBox } from 'element-plus'
import { getAccessibleMenus } from '../utils/permission'
import { checkChartPermission } from '../utils/chartPermission'
import { useUserStore } from '../stores/userStore'
import { 
  DataBoard, 
  Clock, 
  Operation, 
  SwitchButton,
  Menu,
  OfficeBuilding,
  User,
  Setting,
  TrendCharts,
  Search,
  Document,
  PieChart,
  ArrowRight,
  Download,
  Box,
  Message,
  Files
} from '@element-plus/icons-vue'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const currentMenu = ref('1')
    const isCollapse = ref(window.innerWidth <= 960)
    const statsData = ref({})
    const pendingTasks = ref([])
    const recentActivities = ref([])
    const myApplicationCount = ref(0)
    const myAssetCount = ref(0)
    const usingAssetCount = ref(0)
    const idleAssetCount = ref(0)
    const maintenanceAssetCount = ref(0)
    const recentApplications = ref([])
    
    // 搜索关键词
    const searchKeyword = ref('')
    
    const username = computed(() => (userStore.user ? userStore.user.username : ''))
    const userDepartment = computed(() => (userStore.user ? (userStore.user.departmentName || userStore.user.department) : '') || '公司')
    const greeting = computed(() => {
      const hour = new Date().getHours()
      if (hour < 6) return '夜深了'
      if (hour < 12) return '早上好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    })
    // 获取当前用户角色（统一从 store 取）
    const getUserRoleFromLocalStorage = () => {
      return userStore.currentUserRole
    }
    
    const accessibleMenus = computed(() => {
      return getAccessibleMenus()
    })
    
    const totalAssetCount = ref(0)
    const totalAssetValue = ref(0)
    const departmentStats = ref([])
    const statusDistribution = ref([])
    const statusTotal = computed(() => statusDistribution.value.reduce((sum, item) => sum + (Number(item.count) || 0), 0))

    const statusChartRef = ref(null)
    const valueChartRef = ref(null)
    const departmentChartRef = ref(null)
    const statusChart = ref(null)
    const valueChart = ref(null)
    const departmentChart = ref(null)
    
    // 获取用户显示名称（优先显示真实姓名，否则显示用户名）
    const getUserName = () => {
      const user = userStore.user
      if (user) {
        if (user.realName && user.realName.trim()) return user.realName.trim()
        if (user.username && user.username.trim()) return user.username.trim()
      }
      return '用户'
    }

    const getRoleName = () => {
      const roleMap = {
        admin: '系统管理员',
        manager: '部门资产管理员',
        leader: '领导',
        user: '普通员工',
      }
      return roleMap[userStore.currentUserRole] || '用户'
    }

    const getMenuIcon = (menuId) => {
      const iconMap = {
        1: DataBoard,
        2: OfficeBuilding,
        3: User,
        4: Menu,
        5: Setting,
        8: Message, // AI 知识助手
        31: OfficeBuilding, // 部门管理
        32: Document, // 资产分类管理
        33: User, // 员工信息管理
        39: User // 供应商管理
      }
      return iconMap[menuId] || Menu
    }
    
    const getMenuTitle = (menu) => {
      if (menu.titleByRole) {
        const userRole = getUserRoleFromLocalStorage()
        if (menu.titleByRole[userRole]) {
          return menu.titleByRole[userRole]
        }
      }
      return menu.title
    }
    
    const findMenuById = (menus, id) => {
      for (const menu of menus) {
        if (String(menu.id) === id) {
          return menu
        }
        if (menu.children) {
          const found = findMenuById(menu.children, id)
          if (found) {
            return found
          }
        }
      }
      return null
    }

    const handleMenuSelect = (index) => {
      currentMenu.value = index
      const menu = findMenuById(accessibleMenus.value, index)
      if (menu && menu.path) {
        navigateTo(menu.path)
      }
    }
    
    const navigateTo = (path) => {
      router.push(path)
    }
    
    const logout = () => {
      ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 统一通过 store 登出（清空 store + localStorage）
        userStore.logout()
        router.push('/')
      }).catch(() => {
        // 取消退出登录
      })
    }
    
    const exportDepartmentStats = () => {
      // 这里可以添加导出部门资产统计数据的逻辑
      // 例如调用后端API导出Excel文件
    }
    
    const handleSearch = () => {
      // 导航到资产列表页面并传递搜索参数
      router.push({
        path: '/assets',
        query: {
          keyword: searchKeyword.value
        }
      })
    }
    
    const goToMyApplications = () => {
      router.push({ path: '/assets/apply', query: { onlyRecords: 'true' } })
    }
    
    const goToMyAssets = () => {
      router.push({ path: '/assets/list', query: { onlyMyAssets: 'true' } })
    }
    
    const loadMyApplicationCount = async () => {
      try {
        const user = userStore.user
        if (user) {
          const userRole = userStore.currentUserRole
          
          let totalCount = 0
          
          if (userRole === 'leader') {
            // 领导：查看需要自己审批的申请
            const departmentId = user.departmentId || user.deptId
            
            // 获取待审批的资产申请（终审）
            if (departmentId) {
              const assetApprovalResponse = await axios.get(`/asset-applications/status/pending_leader/department/${departmentId}`)
              if (assetApprovalResponse.data.code === 200) {
                totalCount += assetApprovalResponse.data.data.length
              }
            }
            // 获取待审批的采购申请
            const purchaseApprovalResponse = await axios.get(`/purchase-requests/pending/approver/${user.id}`)
            if (purchaseApprovalResponse.data.code === 200) {
              totalCount += purchaseApprovalResponse.data.data.length
            }
          } else if (userRole === 'admin') {
            // 管理员：查看所有待审批申请
            const purchaseResponse = await axios.get('/purchase-requests/status/pending')
            if (purchaseResponse.data.code === 200) {
              totalCount += purchaseResponse.data.data.length
            }
            
            // 统计待审批的资产申请（等待资产管理员审批）
            const assetPendingResponse = await axios.get('/asset-applications/status/pending')
            if (assetPendingResponse.data.code === 200) {
              totalCount += assetPendingResponse.data.data.length
            }
            
            // 统计待终审的资产申请（等待领导审批，如报废申请）
            const assetLeaderResponse = await axios.get('/asset-applications/status/pending_leader')
            if (assetLeaderResponse.data.code === 200) {
              totalCount += assetLeaderResponse.data.data.length
            }
          } else if (userRole === 'manager') {
            // 部门资产管理员：查看需要自己审批的申请（本部门员工的资产申请，排除自己提交的）
            const departmentId = user.departmentId || user.deptId
            if (departmentId) {
              const assetResponse = await axios.get(`/asset-applications/status/pending/department/${departmentId}`)
              if (assetResponse.data.code === 200) {
                // 过滤掉自己提交的申请
                const filteredData = assetResponse.data.data.filter(item => item.applicantId !== user.id)
                totalCount += filteredData.length
              }
            }
          } else {
            // 普通员工：统计自己仍在审批中的资产申请（采购申请接口不对普通员工开放）
            const assetResponse = await axios.get(`/asset-applications/applicant/${user.id}`)
            if (assetResponse.data.code === 200) {
              const payload = assetResponse.data.data
              const applications = Array.isArray(payload) ? payload : (payload?.content || [])
              totalCount += applications.filter(
                item => !['approved', 'rejected'].includes(item.status)
              ).length
            }
          }
          
          myApplicationCount.value = totalCount
        }
      } catch (error) {
        console.error('获取我的申请数量失败', error)
      }
    }
    
    const loadMyAssetCount = async () => {
      // 只有普通员工才需要加载我的资产数量
      if (!userStore.hasRole(['admin', 'leader'])) {
        try {
          const user = userStore.user
          if (user) {
            const token = userStore.token

            // 加载用户资产数量
            const response = await axios.get('/assets/all')

            if (response.data.code === 200) {
              // 过滤出当前用户的资产，包括使用中的、闲置的和维修中的
              const userAssets = response.data.data.filter(asset =>
                asset.userId === user.id &&
                (asset.status === 'using' || asset.status === 'idle' || asset.status === 'maintenance')
              )
              myAssetCount.value = userAssets.length
              
              // 计算不同状态的资产数量
              usingAssetCount.value = userAssets.filter(asset => asset.status === 'using').length
              idleAssetCount.value = userAssets.filter(asset => asset.status === 'idle').length
              maintenanceAssetCount.value = userAssets.filter(asset => asset.status === 'maintenance').length
            }
          }
        } catch (error) {
          console.error('获取我的资产数量失败', error)
        }
      }
    }
    
    const loadRecentApplications = async () => {
      // 只有普通员工才需要加载最近申请
      if (!userStore.hasRole(['admin', 'leader'])) {
        try {
          const user = userStore.user
          if (user) {
            
            // 加载用户最近的申请记录
            const response = await axios.get(`/asset-applications/applicant/${user.id}`)
            
            if (response.data.code === 200) {
              // 处理分页对象或数组
              const data = response.data.data
              const applications = data.content || data
              if (Array.isArray(applications)) {
                // 获取最近5条申请记录
                recentApplications.value = applications.slice(0, 5)
              } else {
                recentApplications.value = []
              }
            }
          }
        } catch (error) {
          console.error('获取最近申请失败', error)
        }
      }
    }
    
    const loadDashboardStats = async () => {
      try {
        // 所有角色都调用同一个接口
        // 后端会根据用户角色和部门ID返回相应的数据
        const response = await axios.get('/dashboard/stats')
        
        if (response.data.code === 200) {
          statsData.value = response.data.data
        } else {
          console.error('获取统计数据失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取统计数据错误:', error)
      }
    }
    
    const loadRecentOperations = async () => {
      try {
        const response = await axios.get('/dashboard/operations', {
          params: {
            limit: 10
          }
        });
        if (response.data.code === 200) {
          pendingTasks.value = response.data.data.pendingTasks || []
          recentActivities.value = response.data.data.recentActivities || []
        } else {
          console.error('获取操作数据失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取操作数据异常:', error)
        // 如果新版API失败，降级到旧版API
        try {
          const oldResponse = await axios.get('/dashboard/recent-operations', {
            params: {
              limit: 10
            }
          })
          if (oldResponse.data.code === 200) {
            recentActivities.value = oldResponse.data.data
          }
        } catch (e) {
          console.error('获取最近操作错误:', e)
        }
      }
    }
    
    const getTypeLabel = (type) => {
      const labels = {
        'purchase': '采购',
        'application': '资产',
        'inventory': '盘点',
        'asset': '资产',
        'system': '系统'
      }
      return labels[type] || type
    }
    
    const getActivityTagType = (type) => {
      const types = {
        'purchase': 'primary',
        'application': 'success',
        'inventory': 'info',
        'asset': 'success',
        'system': 'default'
      }
      return types[type] || 'default'
    }
    
    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    const formatPrice = (price) => {
      if (price === null || price === undefined) return '-'
      const amount = parseFloat(price)
      if (!Number.isFinite(amount)) return '-'
      return '¥' + amount.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
    }

    const formatMoneyAxis = (amount) => {
      const value = Number(amount) || 0
      if (Math.abs(value) >= 100000000) return (value / 100000000).toFixed(1) + '亿'
      if (Math.abs(value) >= 10000) return (value / 10000).toFixed(0) + '万'
      return String(value)
    }

    const formatMoneyCompact = (value) => {
      if (value === null || value === undefined) return '-'
      const amount = Number(value)
      if (!Number.isFinite(amount)) return '-'
      if (Math.abs(amount) >= 100000000) return '¥' + (amount / 100000000).toFixed(2) + ' 亿'
      if (Math.abs(amount) >= 10000) return '¥' + (amount / 10000).toFixed(1) + ' 万'
      return '¥' + amount.toFixed(0)
    }
    
    const getApplicationTypeName = (type) => {
      const typeMap = {
        '领用': '资产领用申请',
        'RECEIVE': '资产领用申请',
        'transfer': '资产转移申请',
        'TRANSFER': '资产转移申请',
        'maintenance': '资产维修申请',
        'MAINTENANCE': '资产维修申请',
        'REPAIR': '资产维修申请',
        'scrap': '资产报废申请',
        'DISPOSAL': '资产报废申请'
      }
      return typeMap[type] || type
    }
    
    const getStatusIcon = (status) => {
      const statusLower = status ? status.toLowerCase() : ''
      switch (statusLower) {
        case 'approved':
          return '✓'
        case 'rejected':
          return '✗'
        case 'pending':
          return '○'
        case 'completed':
          return '✓'
        default:
          return '○'
      }
    }
    
    const getApplicationStatusName = (status) => {
      if (!status) return '-'
      const statusLower = status.toLowerCase()
      const statusMap = {
        'pending': '待审批',
        'approved': '已批准',
        'rejected': '已拒绝',
        'completed': '已完成',
        'leader_approved': '领导已批准',
        'final_approval_created': '已创建终审'
      }
      return statusMap[statusLower] || status
    }
    
    const getApplicationStatusType = (status) => {
      if (!status) return 'info'
      const statusLower = status.toLowerCase()
      const typeMap = {
        'pending': 'warning',
        'approved': 'success',
        'rejected': 'danger',
        'completed': 'info',
        'leader_approved': 'success',
        'final_approval_created': 'primary'
      }
      return typeMap[statusLower] || 'info'
    }
    
    const getToken = () => userStore.token
    
    const fetchDepartmentStats = async () => {
      try {
        const userRole = userStore.currentUserRole
        
        let response
        if (userRole === 'admin' || userRole === 'leader' || userRole === 'manager') {
          // 系统管理员、领导和部门资产管理员都调用同一个接口
          // 后端会根据用户角色和部门ID返回相应的数据
          response = await axios.get('/reports/department-stats')
        } else {
          // 普通员工只能查看自己的资产数据
          departmentStats.value = []
          totalAssetCount.value = 0
          totalAssetValue.value = 0
          usingAssetCount.value = 0
          idleAssetCount.value = 0
          return
        }
        
        if (response.data.code === 200) {
          departmentStats.value = response.data.data || []
          
          // 计算总资产数量和价值
          totalAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.assetCount || 0), 0)
          totalAssetValue.value = departmentStats.value.reduce((sum, item) => sum + (item.totalValue || 0), 0)
          usingAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.inUseCount || 0), 0)
          idleAssetCount.value = departmentStats.value.reduce((sum, item) => sum + (item.idleCount || 0), 0)
          
          renderDepartmentChart()
        } else {
          console.error('获取部门资产统计失败:', response.data.message)
        }
      } catch (error) {
        console.error('获取部门资产统计失败', error)
      }
    }
    
    const fetchStatusDistribution = async () => {
      try {
        const response = await axios.get('/reports/status-distribution')
        if (response.data.code === 200) {
          statusDistribution.value = response.data.data
          renderStatusCharts()
        }
      } catch (error) {
        console.error('获取资产状态分布失败', error)
      }
    }

    const renderDepartmentChart = () => {
      // 检查DOM元素是否存在
      if (!departmentChartRef.value) {
        return
      }
      
      // 检查数据是否存在
      if (!departmentStats.value || departmentStats.value.length === 0) {
        return
      }
      
      // 延迟渲染，确保DOM元素已经完全渲染
      setTimeout(() => {
        try {
          if (departmentChart.value) {
            departmentChart.value.dispose()
          }
          
          // 再次检查DOM元素
          if (!departmentChartRef.value) {
            return
          }
          
          departmentChart.value = echarts.init(departmentChartRef.value)
      
      // 获取部门数据
      const departments = departmentStats.value.map(item => item.departmentName)
      const assetCounts = departmentStats.value.map(item => item.assetCount)
      const totalValues = departmentStats.value.map(item => item.totalValue / 1000) // 转换为千单位
      
      // 根据数据量动态调整柱子宽度
      const getBarWidth = () => {
        if (departments.length === 1) {
          return 80; // 单个部门时，使用固定宽度80px
        } else if (departments.length <= 3) {
          return '40%'; // 少量部门时，使用较小百分比
        } else {
          return '50%'; // 正常情况
        }
      }
      
      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross',
              crossStyle: {
                color: 'rgba(20, 24, 33, 0.16)'
              }
            },
            backgroundColor: 'rgba(255, 255, 255, 0.96)',
            borderColor: 'rgba(20, 24, 33, 0.08)',
            borderWidth: 1,
            padding: [9, 12],
            textStyle: { color: '#2c3342', fontSize: 12 },
            extraCssText: 'box-shadow: 0 12px 32px rgba(16, 24, 40, 0.12); border-radius: 10px;',
            formatter: function(params) {
              let result = params[0].name + '<br/>';
              for (let i = 0; i < params.length; i++) {
                result += params[i].marker + params[i].seriesName + ': ' +
                  (params[i].seriesIndex === 0 ? params[i].value + ' 件' : formatPrice(params[i].value * 1000)) + '<br/>';
              }
              return result;
            }
          },
          legend: {
            data: ['资产数量', '资产价值'],
            bottom: 0,
            icon: 'circle',
            itemWidth: 8,
            itemHeight: 8,
            itemGap: 18,
            textStyle: { color: '#626b7c', fontSize: 11 }
          },
          grid: {
            left: departments.length === 1 ? '25%' : '3%',
            right: departments.length === 1 ? '25%' : '4%',
            bottom: '15%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: departments,
            axisLine: { lineStyle: { color: 'rgba(20, 24, 33, 0.1)' } },
            axisTick: { show: false },
            axisLabel: {
              rotate: departments.length > 4 ? 40 : 0,
              color: '#8b93a3',
              fontSize: 10
            }
          },
          yAxis: [
            {
              type: 'value',
              name: '资产数量',
              min: 0,
              nameTextStyle: { color: '#8b93a3', fontSize: 10 },
              axisLine: { show: false },
              splitLine: { lineStyle: { color: 'rgba(20, 24, 33, 0.06)' } },
              axisLabel: {
                color: '#8b93a3',
                fontSize: 10,
                formatter: '{value} 件'
              }
            },
            {
              type: 'value',
              name: '资产价值',
              min: 0,
              nameTextStyle: { color: '#8b93a3', fontSize: 10 },
              axisLine: { show: false },
              splitLine: { show: false },
              axisLabel: {
                color: '#8b93a3',
                fontSize: 10,
                formatter: (value) => '¥' + formatMoneyAxis(value * 1000)
              }
            }
          ],
          series: [
            {
              name: '资产数量',
              type: 'bar',
              data: assetCounts,
              barWidth: getBarWidth(),
              itemStyle: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [{
                    offset: 0, color: '#8fa3c4'
                  }, {
                    offset: 1, color: '#3d5680'
                  }]
                },
                borderRadius: [6, 6, 0, 0]
              },
              emphasis: {
                itemStyle: {
                  color: {
                    type: 'linear',
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 1,
                    colorStops: [{
                      offset: 0, color: '#a3b4d0'
                    }, {
                      offset: 1, color: '#46608c'
                    }]
                  }
                }
              }
            },
            {
              name: '资产价值',
              type: 'line',
              yAxisIndex: 1,
              data: totalValues,
              smooth: true,
              itemStyle: {
                color: '#a9885f'
              },
              lineStyle: {
                width: 2,
                color: '#a9885f'
              },
              symbol: 'circle',
              symbolSize: 7,
              emphasis: {
                itemStyle: {
                  color: '#c2a67f',
                  borderColor: '#ffffff',
                  borderWidth: 2
                }
              }
            }
          ]
        }
      
          departmentChart.value.setOption(option)
        } catch (error) {
          console.error('渲染部门资产统计图表失败:', error)
        }
      }, 100)
    }

    const renderStatusCharts = () => {
      // 检查数据是否存在
      if (!statusDistribution.value || statusDistribution.value.length === 0) {
        return
      }
      
      // 状态数量分布
      if (statusChartRef.value) {
        // 添加延迟，确保DOM元素已经完全渲染
        setTimeout(() => {
          try {
            if (statusChart.value) {
              statusChart.value.dispose()
            }
            
            // 再次检查DOM元素
            if (!statusChartRef.value) {
              return
            }
            
            statusChart.value = echarts.init(statusChartRef.value)
            
            const statusNames = statusDistribution.value.map(item => item.statusName)
            const counts = statusDistribution.value.map(item => item.count)

            // 默认使用饼图
            const option = {
              color: ['#3d5680', '#8fa3c4', '#a9885f', '#7c9a8a', '#b9c0cc', '#8b93a3'],
              tooltip: {
                trigger: 'item',
                backgroundColor: 'rgba(255, 255, 255, 0.96)',
                borderColor: 'rgba(20, 24, 33, 0.08)',
                borderWidth: 1,
                padding: [9, 12],
                textStyle: { color: '#2c3342', fontSize: 12 },
                extraCssText: 'box-shadow: 0 12px 32px rgba(16, 24, 40, 0.12); border-radius: 10px;'
              },
              legend: {
                orient: 'vertical',
                left: 'left',
                top: 'middle',
                icon: 'circle',
                itemWidth: 8,
                itemHeight: 8,
                itemGap: 14,
                textStyle: { color: '#626b7c', fontSize: 11 }
              },
              series: [
                {
                  name: '资产状态',
                  type: 'pie',
                  radius: ['46%', '66%'],
                  center: ['63%', '50%'],
                  avoidLabelOverlap: true,
                  itemStyle: {
                    borderColor: '#ffffff',
                    borderWidth: 3,
                    borderRadius: 5
                  },
                  label: { show: false },
                  data: statusDistribution.value.map(item => ({
                    name: item.statusName,
                    value: item.count
                  })),
                  emphasis: {
                    scale: true,
                    scaleSize: 4,
                    itemStyle: {
                      shadowBlur: 18,
                      shadowColor: 'rgba(20, 24, 33, 0.16)'
                    }
                  }
                }
              ]
            }
            
            statusChart.value.setOption(option)
          } catch (error) {
            console.error('渲染状态数量分布图表失败:', error)
          }
        }, 100)
      }
      
      // 状态价值分布
      if (valueChartRef.value) {
        // 添加延迟，确保DOM元素已经完全渲染
        setTimeout(() => {
          try {
            if (valueChart.value) {
              valueChart.value.dispose()
            }
            
            // 再次检查DOM元素
            if (!valueChartRef.value) {
              return
            }
            
            valueChart.value = echarts.init(valueChartRef.value)
            
            const statusNames = statusDistribution.value.map(item => item.statusName)
            const values = statusDistribution.value.map(item => item.totalValue)
            
            // 确保数据有效
            if (!statusNames || statusNames.length === 0 || !values || values.length === 0) {
              return
            }
            
            // 使用完整的ECharts配置，确保坐标系正确创建
            const option = {
              tooltip: {
                trigger: 'axis',
                axisPointer: {
                  type: 'shadow',
                  shadowStyle: { color: 'rgba(61, 86, 128, 0.04)' }
                },
                backgroundColor: 'rgba(255, 255, 255, 0.96)',
                borderColor: 'rgba(20, 24, 33, 0.08)',
                borderWidth: 1,
                padding: [9, 12],
                textStyle: { color: '#2c3342', fontSize: 12 },
                extraCssText: 'box-shadow: 0 12px 32px rgba(16, 24, 40, 0.12); border-radius: 10px;'
              },
              grid: {
                left: 8,
                right: 14,
                top: 22,
                bottom: 6,
                containLabel: true
              },
              xAxis: {
                type: 'category',
                data: statusNames,
                axisLine: { lineStyle: { color: 'rgba(20, 24, 33, 0.08)' } },
                axisTick: { show: false },
                axisLabel: {
                  color: '#7d8595',
                  fontSize: 11,
                  margin: 12
                }
              },
              yAxis: {
                type: 'log',
                min: 1,
                axisLine: { show: false },
                axisTick: { show: false },
                splitLine: { lineStyle: { color: 'rgba(20, 24, 33, 0.05)', type: 'dashed' } },
                axisLabel: { show: false }
              },
              series: [
                {
                  name: '资产价值',
                  type: 'bar',
                  data: values,
                  barMaxWidth: 26,
                  itemStyle: {
                    borderRadius: [6, 6, 0, 0],
                    color: {
                      type: 'linear',
                      x: 0,
                      y: 0,
                      x2: 0,
                      y2: 1,
                      colorStops: [
                        { offset: 0, color: '#93a7c7' },
                        { offset: 1, color: '#3d5680' }
                      ]
                    }
                  },
                  label: {
                    show: true,
                    position: 'top',
                    distance: 8,
                    color: '#626b7c',
                    fontSize: 11,
                    formatter: (params) => formatMoneyCompact(params.value)
                  },
                  emphasis: {
                    itemStyle: {
                      color: {
                        type: 'linear',
                        x: 0,
                        y: 0,
                        x2: 0,
                        y2: 1,
                        colorStops: [
                          { offset: 0, color: '#a8b8d3' },
                          { offset: 1, color: '#46608c' }
                        ]
                      }
                    }
                  }
                }
              ]
            }
            
            valueChart.value.setOption(option)
          } catch (error) {
            console.error('渲染状态价值分布图表失败:', error)
          }
        }, 100)
      }
    }

    let compactViewport = window.innerWidth <= 960

    const handleResize = () => {
      const compact = window.innerWidth <= 960
      if (compact !== compactViewport) {
        compactViewport = compact
        isCollapse.value = compact
      }

      try {
        // 检查图表实例是否存在且未被销毁
        if (departmentChart.value && typeof departmentChart.value.resize === 'function' && !departmentChart.value.isDisposed()) {
          departmentChart.value.resize()
        }
        if (statusChart.value && typeof statusChart.value.resize === 'function' && !statusChart.value.isDisposed()) {
          statusChart.value.resize()
        }
        if (valueChart.value && typeof valueChart.value.resize === 'function' && !valueChart.value.isDisposed()) {
          valueChart.value.resize()
        }
      } catch (error) {
        // 忽略 resize 错误
      }
    }
    
    onMounted(() => {
      loadDashboardStats()
      loadRecentOperations()
      loadMyApplicationCount()
      loadMyAssetCount()
      loadRecentApplications()
      
      // 只有当用户有权限时才调用相关API
      if (checkChartPermission('department')) {
        fetchDepartmentStats()
      }
      
      if (checkChartPermission('status')) {
        fetchStatusDistribution()
      }
      
      window.addEventListener('resize', handleResize)
    })
    
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      departmentChart.value?.dispose()
      statusChart.value?.dispose()
      valueChart.value?.dispose()
    })
    
    return {
      currentMenu,
      isCollapse,
      accessibleMenus,
      statsData,
      pendingTasks,
      recentActivities,
      myApplicationCount,
      myAssetCount,
      usingAssetCount,
      idleAssetCount,
      maintenanceAssetCount,
      recentApplications,
      userDepartment,
      totalAssetCount,
      totalAssetValue,
      departmentStats,
      statusDistribution,
      statusChartRef,
      valueChartRef,
      departmentChartRef,
      searchKeyword,
      getRoleName,
      getUserName,
      greeting,
      statusTotal,
      getMenuIcon,
      getMenuTitle,
      handleMenuSelect,
      navigateTo,
      goToMyApplications,
      goToMyAssets,
      logout,
      exportDepartmentStats,
      handleSearch,
      formatTime,
      formatPrice,
      formatMoneyCompact,
      getApplicationTypeName,
      getApplicationStatusName,
      getApplicationStatusType,
      getStatusIcon,
      checkChartPermission,
      getUserRoleFromLocalStorage,
      getTypeLabel,
      getActivityTagType,
      DataBoard,
      Clock,
      Operation,
      SwitchButton,
      PieChart,
      TrendCharts,
      OfficeBuilding,
      Document,
      ArrowRight,
      Download,
      Box,
      Message
    }
  }
}
</script>

<style scoped>
.home-container {
  height: 100vh;
  background:
    radial-gradient(1100px 520px at 8% -12%, rgba(61, 86, 128, 0.07), transparent 62%),
    radial-gradient(900px 480px at 102% 104%, rgba(169, 136, 95, 0.06), transparent 58%),
    var(--eam-canvas);
}

/* ---------- Header ---------- */

.header {
  height: 72px;
  padding: 0;
  background: rgba(250, 250, 251, 0.78);
  border-bottom: 1px solid var(--eam-line);
  backdrop-filter: blur(18px) saturate(1.2);
  -webkit-backdrop-filter: blur(18px) saturate(1.2);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  height: 100%;
  padding: 0 28px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.nav-toggle {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--eam-ink-500);
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-sm);
  cursor: pointer;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease);
}

.nav-toggle:hover {
  color: var(--eam-primary);
  background: #fff;
  box-shadow: var(--eam-shadow-sm);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  font-family: var(--eam-font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #efe6d6;
  background: linear-gradient(160deg, #2b3648 0%, #1b2432 100%);
  box-shadow: 0 6px 14px rgba(20, 24, 33, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.brand-copy strong {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--eam-ink-900);
  white-space: nowrap;
}

.brand-copy span {
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--eam-ink-300);
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-search {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 300px;
  height: 40px;
  padding: 0 5px 0 13px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-pill);
  transition: background-color var(--eam-duration) var(--eam-ease),
    border-color var(--eam-duration) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease);
}

.header-search:focus-within {
  background: #fff;
  border-color: rgba(61, 86, 128, 0.26);
  box-shadow: 0 0 0 4px var(--eam-primary-ghost);
}

.header-search-icon {
  flex: 0 0 auto;
  font-size: 14px;
  color: var(--eam-ink-400);
}

.header-search-input {
  flex: 1;
  min-width: 0;
  padding: 0 9px;
  font-family: inherit;
  font-size: 12.5px;
  color: var(--eam-ink-700);
  background: transparent;
  border: none;
  outline: none;
}

.header-search-input::placeholder {
  color: var(--eam-ink-300);
}

.header-search-submit {
  flex: 0 0 auto;
  padding: 6px 12px;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--eam-primary);
  background: transparent;
  border: none;
  border-radius: var(--eam-radius-pill);
  cursor: pointer;
  transition: background-color var(--eam-duration-fast) var(--eam-ease);
}

.header-search-submit:hover {
  background: var(--eam-primary-ghost);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px 5px 6px;
  font-family: inherit;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--eam-radius-pill);
  cursor: pointer;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    border-color var(--eam-duration-fast) var(--eam-ease);
}

.user-chip:hover {
  background: rgba(255, 255, 255, 0.72);
  border-color: var(--eam-line);
}

.avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: #33445f;
  background: linear-gradient(160deg, #e2e7ef 0%, #cbd5e3 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.25;
}

.user-meta strong {
  max-width: 120px;
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--eam-ink-800);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta span {
  font-size: 10.5px;
  color: var(--eam-ink-400);
}

.user-caret {
  margin-left: 2px;
  font-size: 13px;
  color: var(--eam-ink-300);
  transition: color var(--eam-duration-fast) var(--eam-ease);
}

.user-chip:hover .user-caret {
  color: var(--eam-primary);
}

/* ---------- Layout ---------- */

.content {
  height: calc(100vh - 72px);
}

.sidebar {
  background: rgba(250, 250, 251, 0.55);
  border-right: 1px solid var(--eam-line);
  transition: width 0.35s var(--eam-ease);
}

.sidebar-scrim {
  display: none;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 18px 0 26px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-collapsed {
  overflow: hidden;
}

.nav-caption {
  margin: 0;
  padding: 0 22px 10px;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--eam-ink-300);
}

.el-menu-vertical {
  padding: 0 10px;
  background: transparent;
  border-right: none;
}

.sidebar :deep(.el-sub-menu__title),
.sidebar :deep(.el-menu-item) {
  height: 42px;
  margin: 2px 0;
  line-height: 42px;
  font-size: 13px;
  color: var(--eam-ink-500);
  border-radius: var(--eam-radius-sm);
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease);
}

.sidebar :deep(.el-sub-menu__title:hover),
.sidebar :deep(.el-menu-item:hover) {
  color: var(--eam-ink-800);
  background: rgba(255, 255, 255, 0.66);
}

.sidebar :deep(.el-menu-item.is-active) {
  position: relative;
  font-weight: 600;
  color: var(--eam-primary);
  background: #fff;
  box-shadow: var(--eam-shadow-sm);
}

.sidebar :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  top: 11px;
  bottom: 11px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--eam-primary);
}

.sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--eam-primary);
  font-weight: 500;
}

.sidebar :deep(.el-menu-item .el-icon),
.sidebar :deep(.el-sub-menu__title .el-icon) {
  font-size: 16px;
  color: var(--eam-ink-400);
  transition: color var(--eam-duration-fast) var(--eam-ease);
}

.sidebar :deep(.el-menu-item.is-active .el-icon),
.sidebar :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: var(--eam-primary);
}

.sidebar :deep(.el-menu--collapse) {
  width: 64px;
}

.sidebar :deep(.el-menu--collapse .el-menu-item),
.sidebar :deep(.el-menu--collapse .el-sub-menu__title) {
  justify-content: center;
  padding: 0 !important;
}

.sidebar :deep(.el-menu) {
  background: transparent;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 22px;
  padding: 0 12px;
}

.pending-tasks-card,
.recent-activities-card {
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-md);
  box-shadow: none;
}

.pending-tasks-card :deep(.el-card__header),
.recent-activities-card :deep(.el-card__header) {
  padding: 12px 14px;
  border-bottom-color: var(--eam-line-soft);
}

.pending-tasks-card :deep(.el-card__body),
.recent-activities-card :deep(.el-card__body) {
  padding: 12px 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.panel-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  font-size: 14px;
  color: var(--eam-primary);
  background: var(--eam-primary-soft);
  border-radius: 8px;
}

.panel-title > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.panel-title strong {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--eam-ink-800);
}

.panel-title span {
  font-size: 10.5px;
  color: var(--eam-ink-400);
}

.task-count {
  display: grid;
  place-items: center;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  font-size: 11px;
  font-weight: 600;
  color: var(--eam-warning);
  background: var(--eam-warning-soft);
  border-radius: var(--eam-radius-pill);
}

.task-content,
.activity-content {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 7px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--eam-ink-500);
}

.activity-content strong {
  margin-right: 4px;
  font-weight: 600;
  color: var(--eam-ink-700);
}

.task-type,
.activity-type {
  flex-shrink: 0;
}

.sidebar :deep(.el-timeline) {
  padding-left: 2px;
}

.sidebar :deep(.el-timeline-item) {
  padding-bottom: 12px;
}

.sidebar :deep(.el-timeline-item__tail),
.sidebar :deep(.el-timeline-item__node) {
  display: none;
}

.sidebar :deep(.el-timeline-item__content) {
  padding: 0;
}

.sidebar :deep(.el-timeline-item__timestamp) {
  font-size: 10.5px;
  color: var(--eam-ink-300);
}

/* ---------- Main ---------- */

.main-content {
  padding: 30px 32px 48px !important;
  overflow-y: auto;
}

.overview-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  animation: eam-rise 0.6s var(--eam-ease) both;
}

.overview-copy h2 {
  margin: 9px 0 6px;
  font-size: 27px;
  font-weight: 600;
  letter-spacing: -0.035em;
  color: var(--eam-ink-900);
}

.overview-sub {
  margin: 0;
  font-size: 13px;
  color: var(--eam-ink-400);
}

.overview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px;
  font-size: 11px;
  color: var(--eam-ink-400);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-pill);
  backdrop-filter: blur(8px);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--eam-success);
  box-shadow: 0 0 0 0 rgba(63, 125, 100, 0.4);
  animation: live-pulse 2.8s var(--eam-ease-soft) infinite;
}

@keyframes live-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(63, 125, 100, 0.36); }
  50% { box-shadow: 0 0 0 6px rgba(63, 125, 100, 0); }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(188px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfd 100%);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-lg);
  box-shadow: var(--eam-shadow-sm);
  animation: eam-rise 0.65s var(--eam-ease) both;
}

.stat-card:nth-child(2) { animation-delay: 60ms; }
.stat-card:nth-child(3) { animation-delay: 120ms; }
.stat-card:nth-child(4) { animation-delay: 180ms; }
.stat-card:nth-child(5) { animation-delay: 240ms; }

.stat-card::after {
  content: "";
  position: absolute;
  right: -58px;
  bottom: -74px;
  width: 170px;
  height: 170px;
  pointer-events: none;
  background: radial-gradient(circle, rgba(61, 86, 128, 0.09), transparent 70%);
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: rgba(61, 86, 128, 0.2);
  box-shadow: var(--eam-shadow-md);
}

.stat-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  font-size: 18px;
  color: var(--eam-primary);
  background: var(--eam-primary-soft);
  border-radius: 12px;
}

.stat-card:nth-child(2) .stat-icon {
  color: var(--eam-warning);
  background: var(--eam-warning-soft);
}

.stat-card:nth-child(3) .stat-icon {
  color: var(--eam-success);
  background: var(--eam-success-soft);
}

.stat-card:nth-child(4) .stat-icon {
  color: var(--eam-bronze);
  background: var(--eam-bronze-soft);
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--eam-ink-400);
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.035em;
  color: var(--eam-ink-900);
  font-variant-numeric: tabular-nums;
}

.stat-value--money {
  font-size: 23px;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.stat-meta {
  font-size: 10.5px;
  color: var(--eam-ink-300);
}

.stat-card--accent {
  background: linear-gradient(155deg, #1b2432 0%, #2a3549 100%);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 14px 32px rgba(20, 24, 33, 0.18);
}

.stat-card--accent::after {
  background: radial-gradient(circle, rgba(217, 199, 167, 0.18), transparent 70%);
}

.stat-card--accent .stat-icon {
  color: #d9c7a7;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-card--accent .stat-label {
  color: rgba(255, 255, 255, 0.54);
}

.stat-card--accent .stat-value {
  color: #f6f7f9;
}

.stat-card--accent .stat-meta {
  color: rgba(255, 255, 255, 0.32);
}

/* ---------- Panels ---------- */

.panel-card {
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.86);
  border-radius: var(--eam-radius-lg);
  box-shadow: var(--eam-shadow-sm);
}

.panel-card :deep(.el-card__header) {
  padding: 18px 22px;
  border-bottom: 1px solid var(--eam-line-soft);
}

.panel-card :deep(.el-card__body) {
  padding: 20px 22px;
}

.panel-card .panel-title strong {
  font-size: 14px;
}

.panel-card .panel-title span {
  font-size: 11px;
}

.panel-card .panel-icon {
  width: 32px;
  height: 32px;
  font-size: 15px;
  border-radius: 9px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-row .el-col {
  margin-bottom: 20px;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-container--donut {
  position: relative;
}

.donut-center {
  position: absolute;
  top: 50%;
  left: 63%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.donut-center strong {
  font-size: 25px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--eam-ink-900);
  font-variant-numeric: tabular-nums;
}

.donut-center span {
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--eam-ink-300);
}

.department-detail-card :deep(.el-table) {
  font-size: 12px;
  --el-table-row-hover-bg-color: #f4f7fb;
}

.department-detail-card :deep(.el-table .el-table__cell) {
  padding: 10px 0;
}

.department-detail-card :deep(.el-table .cell) {
  padding: 0 7px;
  white-space: nowrap;
}

.department-detail-card :deep(.el-table th.el-table__cell .cell) {
  font-weight: 500;
  font-size: 11.5px;
  color: var(--eam-ink-400);
}

.department-detail-card :deep(.el-table td.el-table__cell) {
  font-variant-numeric: tabular-nums;
}

.recent-applications :deep(.el-timeline) {
  padding-left: 2px;
}

.recent-applications :deep(.el-timeline-item__tail),
.recent-applications :deep(.el-timeline-item__node) {
  display: none;
}

.recent-applications :deep(.el-timeline-item__timestamp) {
  font-size: 11px;
  color: var(--eam-ink-300);
}

.application-card {
  background: var(--eam-surface-muted);
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-md);
  box-shadow: none;
}

.application-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.application-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--eam-ink-800);
}

.application-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--eam-ink-500);
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 7px;
}

.detail-item .el-icon {
  color: var(--eam-ink-400);
}

.asset-no {
  color: var(--eam-ink-300);
  font-size: 11px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 42px 0;
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-bottom: 4px;
  font-size: 18px;
  color: var(--eam-ink-300);
  background: #f3f5f8;
  border: 1px solid var(--eam-line-soft);
  border-radius: 13px;
}

.empty-state strong {
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-500);
}

.empty-state span:last-child {
  font-size: 12px;
  color: var(--eam-ink-300);
}

.recent-activities-card .empty-state {
  padding: 30px 0;
}

/* ---------- Responsive ---------- */

@media (max-width: 1180px) {
  .header-search {
    width: 220px;
  }

  .stats-grid:has(.stat-card--accent) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stats-grid:has(.stat-card--accent) .stat-card--accent {
    grid-column: span 2;
  }

  .main-content {
    padding: 24px 24px 40px !important;
  }
}

@media (max-width: 960px) {
  .brand-copy span {
    display: none;
  }

  .header-search {
    width: 180px;
  }

  .user-meta span {
    display: none;
  }

  .chart-container {
    height: 290px;
  }

  .sidebar {
    position: fixed;
    top: 72px;
    bottom: 0;
    left: 0;
    z-index: 45;
    width: 258px !important;
    background: var(--eam-surface);
    box-shadow: var(--eam-shadow-lg);
    transform: translateX(-102%);
    transition: transform 0.32s var(--eam-ease);
  }

  .sidebar.sidebar-collapsed {
    transform: translateX(-102%);
  }

  .sidebar:not(.sidebar-collapsed) {
    transform: translateX(0);
  }

  .sidebar-scrim {
    position: fixed;
    inset: 72px 0 0 0;
    z-index: 44;
    display: block;
    background: rgba(16, 22, 33, 0.3);
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.32s var(--eam-ease);
  }

  .sidebar-scrim.is-visible {
    opacity: 1;
    pointer-events: auto;
  }
}

@media (max-width: 768px) {
  .header-content {
    gap: 12px;
    padding: 0 16px;
  }

  .header-search {
    display: none;
  }

  .brand-copy {
    display: none;
  }

  .overview-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .overview-copy h2 {
    font-size: 23px;
  }

  .stats-grid,
  .stats-grid:has(.stat-card--accent) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 24px;
  }

  .main-content {
    padding: 20px 16px 32px !important;
  }

  .chart-container {
    height: 260px;
  }

  .panel-card :deep(.el-card__header),
  .panel-card :deep(.el-card__body) {
    padding: 16px;
  }
}

@media (max-width: 520px) {
  .stats-grid,
  .stats-grid:has(.stat-card--accent) {
    grid-template-columns: 1fr;
  }

  .stats-grid:has(.stat-card--accent) .stat-card--accent {
    grid-column: span 1;
  }

  .user-meta {
    display: none;
  }

  .user-caret {
    display: none;
  }
}
</style>


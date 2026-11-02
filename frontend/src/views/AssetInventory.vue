<template>
  <div class="asset-inventory-container">
    <!-- 导航栏卡片 -->
    <el-card shadow="hover" class="header-card">
      <el-row class="card-header" :gutter="20">
        <el-col :span="4">
          <el-button type="info" @click="goBack" :icon="ArrowLeft">返回首页</el-button>
        </el-col>
        <el-col :span="16" class="text-center">
          <el-tag type="info" size="large" class="title-tag">{{ pageTitle }}</el-tag>
        </el-col>
        <el-col :span="4" class="text-right">
          <el-button type="primary" @click="openCreatePlanDialog" v-if="canCreatePlan" :icon="Plus">创建盘点计划</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover" class="inventory-card" style="margin-top: 20px;">

      <!-- 视图切换和待办提示（仅管理员、领导、资产管理员可见） -->
      <div class="view-switcher" v-if="canSwitchView">
        <el-radio-group v-model="isExecutorView" size="default">
          <el-radio-button :value="true">
            <el-badge :value="myPendingTaskCount" :hidden="myPendingTaskCount === 0" type="danger">
              我的任务
            </el-badge>
          </el-radio-button>
          <el-radio-button :value="false">全部任务</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 执行人专属视图 -->
      <div v-if="isExecutorView && myInProgressTasks.length > 0" class="executor-view">
        <el-alert
          :title="'您有 ' + myInProgressTasks.length + ' 个盘点任务待执行'"
          type="info"
          :closable="false"
          show-icon
          class="task-alert"
        />
        
        <!-- 待执行任务卡片 -->
        <div class="task-cards">
          <el-card 
            v-for="task in myInProgressTasks" 
            :key="task.id" 
            class="task-card"
            :class="{ 'pending': task.status === 'pending', 'in-progress': task.status === 'in_progress' }"
          >
            <template #header>
              <div class="task-card-header">
                <span class="task-status">
                  <el-tag v-if="task.status === 'pending'" type="warning" effect="dark">待执行</el-tag>
                  <el-tag v-else-if="task.status === 'in_progress'" type="success" effect="dark">进行中</el-tag>
                </span>
                <span class="task-name">{{ task.inventoryName }}</span>
              </div>
            </template>
            <div class="task-card-body">
              <div class="task-info">
                <p><strong>盘点范围：</strong>{{ task.inventoryScope }}</p>
                <p><strong>盘点区域：</strong>{{ task.inventoryArea || '未指定' }}</p>
                <p><strong>创建人：</strong>{{ task.creatorName }}</p>
                <p v-if="task.expectedCompletionTime"><strong>截止时间：</strong>{{ formatDateTime(task.expectedCompletionTime) }}</p>
              </div>
              <div class="task-actions">
                <el-button type="primary" @click="viewPlan(task)">查看详情</el-button>
                <el-button 
                  v-if="task.status === 'pending'" 
                  type="success" 
                  @click="startTask(task)"
                >
                  开始盘点
                </el-button>
                <el-button 
                  v-else-if="task.status === 'in_progress' && !task.startTime" 
                  type="success" 
                  @click="startTask(task)"
                >
                  开始盘点
                </el-button>
                <el-button 
                  v-else-if="task.status === 'in_progress' && task.startTime" 
                  type="warning" 
                  @click="viewPlan(task)"
                >
                  继续盘点
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 执行人视图：没有任务时的提示 -->
      <div v-else-if="isExecutorView && myInProgressTasks.length === 0" class="empty-view">
        <el-empty description="暂无待执行的盘点任务">
          <!-- 只有普通用户才显示这个按钮，管理员可以通过"全部任务"切换查看 -->
          <el-button v-if="currentUserRole === 'user'" type="primary" @click="isExecutorView = false">查看我的历史盘点任务</el-button>
        </el-empty>
      </div>

      <!-- 盘点计划列表 -->
      <div class="table-wrapper">
        <el-table v-if="!isExecutorView" :data="inventoryPlans" style="width: 100%" v-loading="loading" :cell-style="{padding: '8px 12px'}">
        <el-table-column prop="inventoryNo" label="盘点编号" min-width="120" />
        <el-table-column prop="inventoryName" label="盘点名称" min-width="140" />
        <el-table-column prop="inventoryDate" label="盘点日期" min-width="120">
          <template #default="scope">
            {{ formatDate(scope.row.inventoryDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="inventoryArea" label="盘点区域" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" min-width="80" />
        <el-table-column prop="createTime" label="创建时间" min-width="140">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="150">
          <template #default="scope">
            <el-button size="small" @click="viewPlan(scope.row)">查看</el-button>
            <el-button size="small" type="primary" @click="assignTask(scope.row)" v-if="scope.row.status === 'pending' && canAssignTask(scope.row)">分配任务</el-button>
            <el-button size="small" type="success" @click="startTask(scope.row)" v-if="scope.row.status === 'in_progress' && !scope.row.startTime && isMyAssignedTask(scope.row)">开始盘点</el-button>
            <el-button size="small" type="warning" @click="completeTask(scope.row)" v-if="scope.row.status === 'in_progress' && scope.row.startTime && hasEditPermission(scope.row)">完成盘点</el-button>
            <el-button size="small" type="danger" @click="deletePlan(scope.row.id)" v-if="currentUserRole === 'admin' || scope.row.creatorId === currentUser.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <!-- 创建盘点计划对话框 -->
      <el-dialog
        v-model="createDialogVisible"
        title="创建盘点计划"
        width="500px"
      >
        <el-form :model="planForm" :rules="planRules" ref="planFormRef">
          <el-form-item label="盘点名称" prop="inventoryName">
            <el-input v-model="planForm.inventoryName" placeholder="请输入盘点名称" />
          </el-form-item>
          <el-form-item label="盘点日期" prop="inventoryDate">
            <el-date-picker
              v-model="planForm.inventoryDate"
              type="date"
              placeholder="选择盘点日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="盘点区域" prop="inventoryArea">
            <el-select v-model="planForm.inventoryArea" placeholder="请选择盘点区域">
              <el-option label="全部区域" value="全部区域" />
              <el-option
                v-for="loc in locations"
                :key="loc"
                :label="loc"
                :value="loc"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="盘点范围" prop="inventoryScope">
            <el-select v-model="planForm.inventoryScope" placeholder="请选择盘点范围">
              <!-- 管理员可以选择全部资产 -->
              <el-option v-if="currentUserRole === 'admin'" label="全部资产" value="全部资产" />
              <!-- 所有部门（根据角色过滤） -->
              <el-option
                v-for="dept in filteredDepartments"
                :key="dept.id"
                :label="dept.deptName"
                :value="dept.deptName"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="预计完成时间" prop="expectedCompletionTime">
            <el-date-picker
              v-model="planForm.expectedCompletionTime"
              type="datetime"
              placeholder="选择预计完成时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="planForm.remark"
              type="textarea"
              placeholder="请输入备注信息"
              rows="3"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="createDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitPlanForm">确定</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 分配任务对话框 -->
      <el-dialog
        v-model="assignDialogVisible"
        title="分配盘点任务"
        width="400px"
      >
        <el-form :model="assignForm" :rules="assignRules" ref="assignFormRef">
          <el-form-item label="负责人" prop="assigneeId">
            <el-select v-model="assignForm.assigneeId" placeholder="请选择负责人">
              <el-option
                v-for="user in filteredUsers"
                :key="user.id"
                :label="user.realName || user.username"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="盘点区域" prop="inventoryArea">
            <el-select v-model="assignForm.inventoryArea" placeholder="请选择盘点区域">
              <el-option
                v-for="loc in locations"
                :key="loc"
                :label="loc"
                :value="loc"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="assignDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitAssignForm">确定</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 查看盘点计划对话框 -->
      <el-dialog
        v-model="viewDialogVisible"
        title="盘点计划详情"
        width="95%"
        max-width="1200px"
      >
        <el-descriptions :column="2">
          <el-descriptions-item label="盘点编号">{{ currentPlan.inventoryNo }}</el-descriptions-item>
          <el-descriptions-item label="盘点名称">{{ currentPlan.inventoryName }}</el-descriptions-item>
          <el-descriptions-item label="盘点日期">{{ formatDate(currentPlan.inventoryDate) }}</el-descriptions-item>
          <el-descriptions-item label="盘点区域">{{ currentPlan.inventoryArea }}</el-descriptions-item>
          <el-descriptions-item label="盘点范围">{{ currentPlan.inventoryScope }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(currentPlan.status) }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentPlan.assigneeName }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ currentPlan.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentPlan.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="预计完成时间">{{ formatDateTime(currentPlan.expectedCompletionTime) }}</el-descriptions-item>
          <el-descriptions-item label="实际完成时间">{{ formatDateTime(currentPlan.actualCompletionTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentPlan.remark }}</el-descriptions-item>
        </el-descriptions>

        <!-- 盘点明细 -->
        <div class="mt-4">
          <div class="detail-header">
            <h4>盘点明细</h4>
            <span v-if="currentPlan.status === 'in_progress' && currentPlan.startTime && hasEditPermission(currentPlan)">
              提示：可以编辑实际数量和备注
            </span>
            <span v-else-if="currentPlan.status === 'in_progress' && currentPlan.startTime">
              提示：您没有编辑权限（仅负责人或管理员可编辑）
            </span>
          </div>
          <el-table :data="inventoryDetails" style="width: 100%; border-collapse: collapse; font-size: 12px;">
            <el-table-column prop="assetId" label="资产ID" width="80" />
            <el-table-column prop="assetNo" label="资产编号" width="140" />
            <el-table-column prop="assetName" label="资产名称" min-width="200" />
            <el-table-column prop="systemQuantity" label="预期数量" width="80" />
            <el-table-column label="实际数量" width="120">
              <template #default="scope">
                <el-input-number
                  v-if="currentPlan.status === 'in_progress' && currentPlan.startTime && isMyAssignedTask(currentPlan)"
                  v-model="scope.row.actualQuantity"
                  :min="0"
                  size="small"
                  style="width: 100%"
                  @change="updateInventoryDetail(scope.row)"
                />
                <span v-else>{{ scope.row.actualQuantity }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getDetailStatusType(scope.row.status)" size="small">
                  {{ getDetailStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="180">
              <template #default="scope">
                <el-input
                  v-if="currentPlan.status === 'in_progress' && currentPlan.startTime && isMyAssignedTask(currentPlan)"
                  v-model="scope.row.remark"
                  type="textarea"
                  :rows="1"
                  placeholder="添加备注"
                  size="small"
                  @change="updateInventoryDetail(scope.row)"
                />
                <span v-else>{{ scope.row.remark }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 操作按钮 -->
        <div class="mt-4" style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button
            v-if="currentPlan.status === 'in_progress' && currentPlan.startTime && isMyAssignedTask(currentPlan)"
            type="primary"
            @click="confirmCompleteTask"
          >
            结束盘点
          </el-button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import { getCurrentUserRole } from '../utils/permission'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

// 当前登录用户
const currentUser = ref(userStore.user || {})
const currentUserRole = ref(getCurrentUserRole())

// 当前用户部门
const currentUserDept = ref('')

// 计算属性：过滤后的部门列表
const filteredDepartments = computed(() => {
  if (currentUserRole.value === 'admin') {
    // 管理员可以看到所有部门
    return departments.value
  } else {
    // 非管理员只能看到本部门
    return departments.value.filter(dept => dept.deptName === currentUserDept.value)
  }
})

// 计算属性：过滤后的用户列表（用于分配任务）
const filteredUsers = computed(() => {
  if (!currentPlan.value) return []
  
  // 获取盘点范围对应的部门
  let targetDept = ''
  if (currentPlan.value.inventoryScope === '全部资产') {
    targetDept = '' // 全部资产不限制部门
  } else {
    targetDept = currentPlan.value.inventoryScope
  }

  // 判断当前用户是否是管理员
  const isAdmin = currentUserRole.value === 'admin'
  
  // 判断当前用户是否是创建者
  const isCreator = currentPlan.value.creatorId && currentPlan.value.creatorId === currentUser.value.id

  // 根据部门名称获取部门ID
  const getDeptIdByName = (deptName) => {
    if (!deptName) return null
    const dept = departments.value.find(d => d.deptName === deptName)
    return dept ? dept.id : null
  }

  const targetDeptId = getDeptIdByName(targetDept)

  // 系统管理员可以分配任务给盘点范围部门的所有人
  if (isAdmin) {
    if (targetDeptId) {
      return users.value.filter(user => {
        return user.deptId === targetDeptId
      })
    } else {
      // 全部资产：可以分配给所有用户
      return users.value
    }
  } 
  
  // 获取用户角色的辅助函数
  const getUserRoleFromUser = (user) => {
    if (!user) return 'user'
    if (user.role) {
      const roleName = user.role.toLowerCase()
      if (roleName === 'admin' || roleName === 'leader' || roleName === 'manager' || roleName === 'user') {
        return roleName
      }
    }
    return 'user'
  }
  
  // 部门领导创建的计划：只能分配给本部门的资产管理员和普通用户
  if (isCreator && currentUserRole.value === 'leader') {
    const userDeptId = getDeptIdByName(currentUserDept.value)
    return users.value.filter(user => {
      const userRole = getUserRoleFromUser(user)
      return (userRole === 'manager' || userRole === 'user') && 
             user.deptId === userDeptId
    })
  }
  
  // 部门资产管理员创建的计划：只能分配给本部门的普通用户
  if (isCreator && currentUserRole.value === 'manager') {
    const userDeptId = getDeptIdByName(currentUserDept.value)
    return users.value.filter(user => {
      const userRole = getUserRoleFromUser(user)
      return userRole === 'user' && user.deptId === userDeptId
    })
  }
  
  return []
})

// 检查是否有创建盘点计划的权限
const canCreatePlan = computed(() => {
  return currentUserRole.value === 'admin' || 
         currentUserRole.value === 'leader' || 
         currentUserRole.value === 'manager'
})

// 页面标题：根据角色和视图动态显示
const pageTitle = computed(() => {
  if (isExecutorView.value) {
    return '我的盘点任务'
  }
  // 普通用户查看历史任务时显示"我的历史盘点任务"
  if (currentUserRole.value === 'user') {
    return '我的历史盘点任务'
  }
  return '资产盘点管理'
})

// 检查是否有编辑权限（负责人或管理员）
const hasEditPermission = (plan) => {
  if (!plan || !currentUser.value.id) return false
  // 管理员可以编辑所有
  if (currentUserRole.value === 'admin') return true
  // 负责人可以编辑自己负责的盘点
  return plan.creatorId === currentUser.value.id || plan.assigneeId === currentUser.value.id
}

// 检查是否有分配任务的权限
const canAssignTask = (plan) => {
  if (!plan || !currentUser.value.id) return false
  // 管理员可以分配所有任务
  if (currentUserRole.value === 'admin') return true
  // 只有创建者本人可以分配任务
  return plan.creatorId === currentUser.value.id
}

// 是否是分配给我的任务
const isMyAssignedTask = (plan) => {
  return plan.assigneeId === currentUser.value.id
}

// 是否是执行人视图（普通用户默认且只能看到自己的任务）
const isExecutorView = ref(currentUserRole.value === 'user')

// 普通用户只能看到自己的任务，不能切换视图
const canSwitchView = computed(() => {
  return currentUserRole.value !== 'user'  // 只有管理员、领导、资产管理员可以切换视图
})

// 分配给我的待处理任务数量
const myPendingTaskCount = computed(() => {
  return inventoryPlans.value.filter(p => 
    p.assigneeId === currentUser.value.id && p.status === 'pending'
  ).length
})

// 分配给我的进行中的任务
const myInProgressTasks = computed(() => {
  return inventoryPlans.value.filter(p => 
    p.assigneeId === currentUser.value.id && 
    (p.status === 'in_progress' || p.status === 'pending')
  )
})

// 根据视图返回不同的列表
const displayPlans = computed(() => {
  if (isExecutorView.value) {
    // 执行人视图：只显示分配给我的任务
    return inventoryPlans.value.filter(p => p.assigneeId === currentUser.value.id)
  } else {
    // 全部任务视图
    return inventoryPlans.value
  }
})

// 状态管理
const loading = ref(false)
const inventoryPlans = ref([])
const users = ref([])
const inventoryDetails = ref([])
const locations = ref([])
const departments = ref([])

// 对话框状态
const createDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const viewDialogVisible = ref(false)

// 表单数据
const planForm = ref({
  inventoryName: '',
  inventoryDate: '',
  inventoryArea: '',
  inventoryScope: '',
  expectedCompletionTime: '',
  remark: ''
})

const assignForm = ref({
  assigneeId: '',
  assigneeName: '',
  inventoryArea: ''
})

const currentPlan = ref({})

// 表单验证规则
const planRules = {
  inventoryName: [
    { required: true, message: '请输入盘点名称', trigger: 'blur' }
  ],
  inventoryDate: [
    { required: true, message: '请选择盘点日期', trigger: 'change' }
  ],
  inventoryScope: [
    { required: true, message: '请输入盘点范围', trigger: 'blur' }
  ]
}

const assignRules = {
  assigneeId: [
    { required: true, message: '请选择负责人', trigger: 'blur' }
  ],
  inventoryArea: [
    { required: true, message: '请输入盘点区域', trigger: 'blur' }
  ]
}

// 表单引用
const planFormRef = ref(null)
const assignFormRef = ref(null)

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString()
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString()
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'info'
    case 'in_progress': return 'warning'
    case 'completed': return 'success'
    case 'cancelled': return 'danger'
    default: return ''
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'pending': return '待处理'
    case 'in_progress': return '进行中'
    case 'completed': return '已完成'
    case 'cancelled': return '已取消'
    default: return status
  }
}

// 获取明细状态类型
const getDetailStatusType = (status) => {
  switch (status) {
    case 'pending': return 'info'
    case 'normal': return 'success'
    case 'surplus': return 'warning'
    case 'shortage': return 'danger'
    default: return ''
  }
}

// 获取明细状态文本
const getDetailStatusText = (status) => {
  switch (status) {
    case 'pending': return '待盘点'
    case 'normal': return '正常'
    case 'surplus': return '盘盈'
    case 'shortage': return '盘亏'
    default: return status
  }
}

// 更新盘点明细
const updateInventoryDetail = async (detail) => {
  try {
    const response = await axios.put(`/asset-inventory/details/${detail.id}`, detail)
    if (response.data.code !== 200) {
      ElMessage.error('更新盘点明细失败')
    }
  } catch (error) {
    console.error('更新盘点明细失败', error)
    ElMessage.error('更新盘点明细失败')
  }
}

// 返回首页
const goBack = () => {
  router.push('/home')
}

// 获取盘点计划列表
const fetchInventoryPlans = async () => {
  loading.value = true
  try {
    let response
    // 系统管理员可以看到所有盘点计划，其他角色只能看到自己创建的和分配给自己的
    if (currentUserRole.value === 'admin') {
      response = await axios.get('/asset-inventory')
    } else {
      response = await axios.get(`/asset-inventory/user/${currentUser.value.id}`)
    }
    if (response.data.code === 200) {
      inventoryPlans.value = response.data.data
    } else {
      ElMessage.error('获取盘点计划失败')
    }
  } catch (error) {
    ElMessage.error('获取盘点计划失败')
  } finally {
    loading.value = false
  }
}

// 获取用户列表
const fetchUsers = async () => {
  try {
    const response = await axios.get('/users')
    if (response.data.code === 200) {
      users.value = response.data.data
    }
  } catch (error) {
    // 忽略错误
  }
}

// 获取地点列表
const fetchLocations = async () => {
  try {
    const response = await axios.get('/assets/locations')
    if (response.data.code === 200) {
      locations.value = response.data.data
    }
  } catch (error) {
    // 忽略错误
  }
}

// 获取部门列表
const fetchDepartments = async () => {
  try {
    const response = await axios.get('/departments')
    if (response.data.code === 200) {
      departments.value = response.data.data
    }
  } catch (error) {
    // 忽略错误
  }
}

// 打开创建盘点计划对话框
const openCreatePlanDialog = () => {
  planForm.value = {
    inventoryName: '',
    inventoryDate: '',
    inventoryArea: '',
    inventoryScope: '',
    expectedCompletionTime: '',
    remark: ''
  }
  
  // 非管理员用户自动设置盘点范围为本部门
  if (currentUserRole.value !== 'admin' && filteredDepartments.value.length > 0) {
    planForm.value.inventoryScope = filteredDepartments.value[0].deptName
  }
  
  createDialogVisible.value = true
}

// 提交创建盘点计划
const submitPlanForm = async () => {
  if (!planFormRef.value.validate()) return

  try {
    const user = userStore.user

    // 处理日期格式转换
    // inventoryDate: 转换为 yyyy-MM-dd 格式（后端 LocalDate 类型）
    const inventoryDateStr = planForm.value.inventoryDate 
      ? new Date(planForm.value.inventoryDate).toISOString().split('T')[0] 
      : ''
    // expectedCompletionTime: 转换为 yyyy-MM-dd'T'HH:mm:ss 格式（后端 LocalDateTime 类型，必须带T）
    const expectedCompletionTimeStr = planForm.value.expectedCompletionTime 
      ? new Date(planForm.value.expectedCompletionTime).toISOString().slice(0, 19)  // 保持T分隔符
      : ''
    
    // 构建请求数据
    const requestData = {
      inventoryName: planForm.value.inventoryName,
      inventoryDate: inventoryDateStr,
      inventoryArea: planForm.value.inventoryArea,
      inventoryScope: planForm.value.inventoryScope,
      expectedCompletionTime: expectedCompletionTimeStr,
      remark: planForm.value.remark || '',
      creatorId: Number(user.id),  // 确保是数字类型
      creatorName: user.realName || user.username
    }
    
    const response = await axios.post('/asset-inventory', requestData)

    if (response.data.code === 200) {
      ElMessage.success('创建盘点计划成功')
      createDialogVisible.value = false
      fetchInventoryPlans()
    } else {
      console.error('业务错误:', response.data)
      ElMessage.error('创建盘点计划失败: ' + (response.data.message || response.data.msg || '未知错误'))
    }
  } catch (error) {
    console.error('========== 请求异常 ==========')
    console.error('错误对象:', error)
    console.error('错误消息:', error.message)
    if (error.response) {
      console.error('响应状态码:', error.response.status)
      console.error('响应头:', error.response.headers)
      console.error('响应数据:', error.response.data)
    } else if (error.request) {
      console.error('请求对象:', error.request)
    }
    console.error('==============================')
    ElMessage.error('创建盘点计划失败: ' + (error.response?.data?.message || error.response?.data?.msg || error.message))
  }
}

// 分配任务
const assignTask = (plan) => {
  currentPlan.value = plan
  assignForm.value = {
    assigneeId: '',
    assigneeName: '',
    inventoryArea: plan.inventoryArea || ''
  }
  assignDialogVisible.value = true
}

// 提交分配任务
const submitAssignForm = async () => {
  if (!assignFormRef.value.validate()) return

  try {
    const selectedUser = users.value.find(user => user.id === assignForm.value.assigneeId)
    const response = await axios.post(`/asset-inventory/${currentPlan.value.id}/assign`, {
      assigneeId: assignForm.value.assigneeId,
      assigneeName: selectedUser.realName || selectedUser.username,
      inventoryArea: assignForm.value.inventoryArea
    })

    if (response.data.code === 200) {
      ElMessage.success('分配任务成功')
      assignDialogVisible.value = false
      fetchInventoryPlans()
    } else {
      ElMessage.error('分配任务失败')
    }
  } catch (error) {
    ElMessage.error('分配任务失败')
  }
}

// 查看盘点计划
const viewPlan = async (plan) => {
  currentPlan.value = plan
  try {
    const response = await axios.get(`/asset-inventory/${plan.id}/details`)
    if (response.data.code === 200) {
      inventoryDetails.value = response.data.data
    }
  } catch (error) {
    // 忽略错误
  }
  viewDialogVisible.value = true
}

// 开始盘点
const startTask = async (plan) => {
  try {
    const response = await axios.post(`/asset-inventory/${plan.id}/start`)
    if (response.data.code === 200) {
      ElMessage.success('开始盘点成功')
      fetchInventoryPlans()
      // 自动打开盘点明细，让用户可以立即开始盘点操作
      viewPlan(plan)
    } else {
      ElMessage.error('开始盘点失败')
    }
  } catch (error) {
    ElMessage.error('开始盘点失败')
  }
}

// 确认结束盘点
const confirmCompleteTask = () => {
  ElMessageBox.confirm(
    '确认要结束本次盘点任务吗？结束后将无法修改盘点数据。',
    '确认结束盘点',
    {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await axios.post(`/asset-inventory/${currentPlan.value.id}/complete`)
      if (response.data.code === 200) {
        ElMessage.success('盘点完成成功')
        fetchInventoryPlans()
        viewDialogVisible.value = false
      } else {
        ElMessage.error('盘点完成失败')
      }
    } catch (error) {
      ElMessage.error('盘点完成失败')
    }
  }).catch(() => {
    ElMessage.info('已取消结束盘点')
  })
}

// 完成盘点
const completeTask = async (plan) => {
  try {
    const response = await axios.post(`/asset-inventory/${plan.id}/complete`)
    if (response.data.code === 200) {
      ElMessage.success('完成盘点成功')
      fetchInventoryPlans()
    } else {
      ElMessage.error('完成盘点失败')
    }
  } catch (error) {
    ElMessage.error('完成盘点失败')
  }
}

// 删除盘点计划
const deletePlan = (id) => {
  ElMessageBox.confirm('确定要删除这个盘点计划吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await axios.delete(`/asset-inventory/${id}`)
      if (response.data.code === 200) {
        ElMessage.success('删除盘点计划成功')
        fetchInventoryPlans()
      } else {
        ElMessage.error('删除盘点计划失败')
      }
    } catch (error) {
      ElMessage.error('删除盘点计划失败')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 初始化
onMounted(async () => {
  // 根据角色决定需要加载的数据
  const fetchPromises = [
    fetchInventoryPlans(),
    fetchUsers(),
    fetchLocations()
  ]
  
  // 只有管理员、领导、资产管理员才需要获取部门列表
  if (currentUserRole.value !== 'user') {
    fetchPromises.push(fetchDepartments())
  }
  
  await Promise.all(fetchPromises)
  
  // 设置当前用户的部门信息（普通用户从用户数据中获取）
  if (users.value.length > 0) {
    const currentUserData = users.value.find(u => u.id === currentUser.value.id)
    if (currentUserData) {
      if (currentUserData.deptId && departments.value.length > 0) {
        // 根据用户的deptId查找部门名称
        const userDept = departments.value.find(d => d.id === currentUserData.deptId)
        currentUserDept.value = userDept ? userDept.deptName : ''
      } else if (currentUserData.deptName) {
        // 如果没有部门列表但用户有deptName字段，直接使用
        currentUserDept.value = currentUserData.deptName
      }
    }
  }
})
</script>

<style scoped>
.asset-inventory-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.card-header {
  align-items: center;
}

.title-tag {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.inventory-card {
  margin-bottom: 20px;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.mt-4 {
  margin-top: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-header h4 {
  margin: 0;
}

.detail-header span {
  color: #909399;
  font-size: 14px;
}

.view-switcher {
  margin-bottom: 20px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.executor-view {
  margin-bottom: 20px;
}

.task-alert {
  margin-bottom: 20px;
}

.task-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.task-card {
  width: 350px;
  border-left: 4px solid #409eff;
}

.task-card.pending {
  border-left-color: #e6a23c;
}

.task-card.in-progress {
  border-left-color: #67c23a;
}

.task-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-status {
  flex-shrink: 0;
}

.task-name {
  font-weight: bold;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card-body {
  padding: 10px 0;
}

.task-info {
  margin-bottom: 15px;
}

.task-info p {
  margin: 8px 0;
  color: #606266;
}

.task-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.empty-view {
  padding: 60px 0;
  text-align: center;
}

/* 表格包装器 - 响应式布局 */
.table-wrapper {
  overflow-x: auto;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.table-wrapper::-webkit-scrollbar {
  height: 8px;
}

.table-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .asset-inventory-container {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .view-switcher {
    padding: 8px;
  }
  
  .task-cards {
    gap: 12px;
  }
  
  .task-card {
    width: 100%;
    min-width: 280px;
  }
}

@media (max-width: 992px) {
  .task-cards {
    gap: 15px;
  }
  
  .task-card {
    width: calc(50% - 8px);
    min-width: 280px;
  }
}

@media (min-width: 1200px) {
  .task-card {
    width: calc(33.33% - 14px);
  }
}
</style>
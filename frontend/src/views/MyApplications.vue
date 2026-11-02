<template>
  <ManagementPage
    title="申请记录"
    :show-add-button="false"
    :show-back-button="true"
    :show-pagination="true"
    :total="total"
    :page-size="pageSize"
    :loading="loading"
    :search-fields="searchFields"
    :columns="tableColumns"
    :data="applications"
    @search="handleSearch"
    @reset="handleReset"
    @page-change="handlePageChange"
  >
    <!-- 自定义申请类型列 -->
    <template #column-applicationType="{ row }">
      <el-tag :type="getTypeTagType(row.applicationType)">
        {{ getTypeText(row.applicationType) }}
      </el-tag>
    </template>
    
    <!-- 自定义状态列 -->
    <template #column-status="{ row }">
      <el-tag :type="getStatusTagType(row.status)">
        {{ getStatusText(row.status) }}
      </el-tag>
    </template>
    
    <!-- 自定义操作列 -->
    <template #actions="{ row }">
      <el-button type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
    </template>
  </ManagementPage>
  
  <!-- 查看详情对话框 -->
  <el-dialog v-model="viewDialogVisible" title="申请详情" width="600px">
    <el-descriptions :column="2" border v-if="viewData">
      <el-descriptions-item label="申请编号">{{ viewData.id }}</el-descriptions-item>
      <el-descriptions-item label="申请类型">
        <el-tag :type="getTypeTagType(viewData.applicationType)">{{ getTypeText(viewData.applicationType) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="物品/资产名称">{{ viewData.assetName || viewData.itemName }}</el-descriptions-item>
      <el-descriptions-item label="规格型号">{{ viewData.specification }}</el-descriptions-item>
      <el-descriptions-item label="数量">{{ viewData.quantity }}</el-descriptions-item>
      <el-descriptions-item label="单位">{{ viewData.unit }}</el-descriptions-item>
      <el-descriptions-item label="申请人">{{ viewData.applicantName }}</el-descriptions-item>
      <el-descriptions-item label="所属部门">{{ viewData.departmentName }}</el-descriptions-item>
      <el-descriptions-item label="申请日期">{{ viewData.applicationDate }}</el-descriptions-item>
      <el-descriptions-item label="申请状态">
        <el-tag :type="getStatusTagType(viewData.status)">{{ getStatusText(viewData.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="审批人">{{ viewData.approverName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审批时间">{{ viewData.approvalDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审批意见" :span="2">{{ viewData.approvalRemark || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ viewData.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import ManagementPage from '../components/business/ManagementPage.vue'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

// 数据
const applications = ref([])
const total = ref(0)
const loading = ref(false)

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索表单
const searchForm = reactive({
  type: '',
  status: ''
})

// 当前用户信息
const currentUser = ref(null)
const userRole = ref('')
const userDepartmentId = ref(null)
const userId = ref(null)

// 详情对话框
const viewDialogVisible = ref(false)
const viewData = ref(null)

// 搜索字段配置
const searchFields = computed(() => [
  {
    prop: 'type',
    label: '申请类型',
    type: 'select',
    width: '150px',
    options: [
      { label: '全部', value: '' },
      { label: '资产领用', value: 'RECEIVE' },
      { label: '资产维修', value: 'REPAIR' },
      { label: '资产转移', value: 'TRANSFER' },
      { label: '资产报废', value: 'DISPOSAL' },
      { label: '采购需求', value: 'PURCHASE' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    width: '150px',
    options: [
      { label: '全部', value: '' },
      { label: '待审批', value: 'pending' },
      { label: '待领导审批', value: 'pending_leader' },
      { label: '领导已批准', value: 'leader_approved' },
      { label: '已批准', value: 'approved' },
      { label: '已拒绝', value: 'rejected' },
      { label: '已完成', value: 'completed' }
    ]
  }
])

// 表格列配置
const tableColumns = computed(() => [
  { prop: 'id', label: '申请编号', width: '100' },
  { prop: 'applicationType', label: '申请类型', width: '120', type: 'custom' },
  { prop: 'assetName', label: '物品/资产名称', minWidth: '150' },
  { prop: 'applicantName', label: '申请人', width: '120' },
  { prop: 'departmentName', label: '所属部门', width: '120' },
  { prop: 'applicationDate', label: '申请日期', width: '180', type: 'time' },
  { prop: 'status', label: '申请状态', width: '120', type: 'custom' },
  { prop: 'approvalRemark', label: '审批意见', minWidth: '150', showOverflowTooltip: true },
  { type: 'actions', label: '操作', width: '120' }
])

// 获取用户角色
const getUserRole = (username) => {
  const roleMap = {
    'admin': 'admin',
    'system': 'admin',
    'leader': 'leader',
    'manager': 'manager'
  }
  const lowerUsername = username.toLowerCase()
  if (roleMap[lowerUsername]) {
    return roleMap[lowerUsername]
  }

  const user = userStore.user
  if (user && user.authorities) {
    if (user.authorities.includes('ROLE_leader') || user.authorities.includes('ROLE_LEADER')) {
      return 'leader'
    }
    if (user.authorities.includes('ROLE_manager') || user.authorities.includes('ROLE_MANAGER')) {
      return 'manager'
    }
  }

  return 'user'
}

// 获取申请类型标签类型
const getTypeTagType = (type) => {
  const typeMap = {
    'RECEIVE': 'primary',
    'REPAIR': 'warning',
    'TRANSFER': 'info',
    'DISPOSAL': 'danger',
    'PURCHASE': 'success'
  }
  return typeMap[type] || 'default'
}

// 获取申请类型文本
const getTypeText = (type) => {
  const typeMap = {
    'RECEIVE': '资产领用',
    'REPAIR': '资产维修',
    'TRANSFER': '资产转移',
    'DISPOSAL': '资产报废',
    'PURCHASE': '采购需求'
  }
  return typeMap[type] || type
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  if (!status) return undefined
  const statusLower = status.toLowerCase()
  const statusMap = {
    'pending': 'warning',
    'pending_leader': 'warning',
    'leader_approved': 'info',
    'approved': 'success',
    'rejected': 'danger',
    'completed': 'success',
    'final_approval_created': 'info',
    'ordered': 'info'
  }
  return statusMap[statusLower] || undefined
}

// 获取状态文本
const getStatusText = (status) => {
  if (!status) return '-'
  const statusLower = status.toLowerCase()
  const statusMap = {
    'pending': '待审批',
    'pending_leader': '待领导审批',
    'leader_approved': '领导已批准',
    'approved': '已批准',
    'rejected': '已拒绝',
    'completed': '已完成',
    'final_approval_created': '已提交终审',
    'ordered': '已下单'
  }
  return statusMap[statusLower] || status
}

// 获取申请记录
const fetchApplications = async (page = 1, size = 10, type = '', status = '') => {
  try {
    const user = userStore.user
    if (!user) {
      router.push('/login')
      return { data: [], total: 0 }
    }
    currentUser.value = user
    userId.value = user.id
    userRole.value = getUserRole(user.username)
    userDepartmentId.value = user.departmentId || user.deptId
    
    let url = '/asset-applications'
    const params = new URLSearchParams()
    params.set('page', page - 1)
    params.set('size', size)
    
    if (type && type !== 'PURCHASE') params.set('type', type)
    if (status) params.set('status', status)
    
    if (userRole.value === 'admin') {
      url = `/asset-applications?${params.toString()}`
    } else if (userRole.value === 'leader' || userRole.value === 'manager') {
      if (userDepartmentId.value) {
        url = `/asset-applications/department/${userDepartmentId.value}?${params.toString()}`
      }
    } else {
      url = `/asset-applications/applicant/${userId.value}?${params.toString()}`
    }
    
    const response = await axios.get(url)
    if (response.data.code === 200) {
      return {
        data: response.data.data.content || response.data.data,
        total: response.data.data.totalElements || response.data.data.length || 0
      }
    }
    return { data: [], total: 0 }
  } catch (error) {
    console.error('获取申请记录失败', error)
    return { data: [], total: 0 }
  }
}

// 获取采购需求申请记录
const fetchPurchaseRequests = async (page = 1, size = 10, type = '', status = '') => {
  try {
    if (!currentUser.value) return { data: [], total: 0 }
    
    let url = '/purchase-requests'
    const params = new URLSearchParams()
    params.set('page', page - 1)
    params.set('size', size)
    
    if (status) params.set('status', status)
    
    if (userRole.value === 'admin') {
      url = `/purchase-requests?${params.toString()}`
    } else if (userRole.value === 'leader' || userRole.value === 'manager') {
      if (userDepartmentId.value) {
        url = `/purchase-requests/department/${userDepartmentId.value}?${params.toString()}`
      }
    } else {
      url = `/purchase-requests/applicant/${userId.value}?${params.toString()}`
    }
    
    const response = await axios.get(url)
    if (response.data.code === 200) {
      const purchaseRequests = response.data.data.content || response.data.data
      const transformed = purchaseRequests.map(item => ({
        ...item,
        applicationType: 'PURCHASE',
        assetName: item.itemName,
        applicantName: item.applicantName,
        departmentName: item.departmentName,
        applicationDate: item.applicationDate,
        status: item.status ? item.status.toLowerCase() : 'pending',
        approvalRemark: item.approvalRemark,
        approverName: item.approverName,
        approvalDate: item.approvalDate
      }))
      return {
        data: transformed,
        total: response.data.data.totalElements || response.data.data.length || 0
      }
    }
    return { data: [], total: 0 }
  } catch (error) {
    console.error('获取采购需求申请失败', error)
    return { data: [], total: 0 }
  }
}

// 获取所有类型的申请记录
const fetchAllApplications = async () => {
  loading.value = true
  applications.value = []
  total.value = 0
  
  const { type, status } = searchForm
  
  try {
    if (type === 'PURCHASE') {
      const result = await fetchPurchaseRequests(currentPage.value, pageSize.value, type, status)
      applications.value = result.data
      total.value = result.total
    } else {
      const assetResult = await fetchApplications(currentPage.value, pageSize.value, type, status)
      applications.value = assetResult.data
      total.value = assetResult.total
      
      if (!type) {
        const purchaseResult = await fetchPurchaseRequests(currentPage.value, pageSize.value, type, status)
        applications.value = [...applications.value, ...purchaseResult.data]
        total.value += purchaseResult.total
      }
    }
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = (row) => {
  viewData.value = row
  viewDialogVisible.value = true
}

// 搜索处理
const handleSearch = (form) => {
  Object.assign(searchForm, form)
  currentPage.value = 1
  fetchAllApplications()
}

// 重置处理
const handleReset = () => {
  searchForm.type = ''
  searchForm.status = ''
  currentPage.value = 1
  fetchAllApplications()
}

// 分页处理
const handlePageChange = ({ pageSize: size, currentPage: page }) => {
  pageSize.value = size
  currentPage.value = page
  fetchAllApplications()
}

onMounted(() => {
  fetchAllApplications()
})
</script>

<style scoped>
</style>

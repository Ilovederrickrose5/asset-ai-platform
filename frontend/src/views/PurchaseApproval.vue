<template>
  <div class="purchase-approval-container">
    <el-card class="header-card">
      <div class="card-header">
        <span class="title">采购需求审批</span>
        <div class="header-buttons">
          <el-button @click="handleBackToHome">返回首页</el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选区域 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <el-select 
          v-model="searchForm.status" 
          placeholder="选择状态" 
          class="status-select"
          @change="handleStatusChange"
        >
          <el-option label="待审批" value="pending" />
          <el-option label="已批准" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="全部" value="all" />
        </el-select>
      </div>
    </el-card>

    <el-card class="content-card">
      <el-table :data="requests" v-loading="loading" stripe :fit="true" border>
        <el-table-column prop="id" label="申请编号" />
        <el-table-column prop="itemName" label="物品名称" />
        <el-table-column prop="specification" label="规格型号" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column prop="estimatedUnitPrice" label="预估单价">
          <template #default="{ row }">
            ¥{{ formatPrice(row.estimatedUnitPrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额">
          <template #default="{ row }">
            ¥{{ formatPrice(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" />
        <el-table-column prop="departmentName" label="部门" />
        <el-table-column prop="applicationDate" label="申请时间">
          <template #default="{ row }">
            {{ formatTime(row.applicationDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 'pending'" link type="success" size="small" @click="handleApprove(row)">批准</el-button>
            <el-button v-if="row.status === 'pending'" link type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="viewDialogVisible" title="查看采购需求申请" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ viewData.id }}</el-descriptions-item>
        <el-descriptions-item label="物品名称">{{ viewData.itemName }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ viewData.specification }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ viewData.quantity }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ viewData.unit }}</el-descriptions-item>
        <el-descriptions-item label="预估单价">¥{{ formatPrice(viewData.estimatedUnitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ formatPrice(viewData.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(viewData.status)">{{ getStatusText(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ viewData.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ viewData.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(viewData.applicationDate) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ viewData.approverName }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ formatTime(viewData.approvalDate) }}</el-descriptions-item>
        <el-descriptions-item label="采购原因" :span="2">{{ viewData.purchaseReason }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ viewData.approvalRemark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="approvalDialogVisible" :title="approvalDialogTitle" width="500px">
      <el-form :model="approvalForm" :rules="approvalRules" ref="approvalFormRef" label-width="100px">
        <el-form-item label="审批意见" prop="approvalRemark">
          <el-input v-model="approvalForm.approvalRemark" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApprovalSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/request'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const requests = ref([])
const viewDialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const approvalDialogTitle = ref('')
const approvalFormRef = ref(null)
const currentUser = ref(null)
const viewData = ref({})
const searchForm = ref({
  status: 'pending'
})
const approvalForm = ref({
  id: null,
  action: '',
  approvalRemark: ''
})

const approvalRules = {
  approvalRemark: [{ required: true, message: '请输入审批意见', trigger: 'blur' }]
}

const getToken = () => userStore.token

const getCurrentUser = () => {
  if (userStore.user) {
    currentUser.value = userStore.user
  }
}

const fetchRequests = async () => {
  loading.value = true
  try {
    let url = ''
    if (searchForm.value.status === 'pending') {
      // 获取待审批的申请
      url = `/purchase-requests/pending/approver/${currentUser.value.id}`
    } else if (searchForm.value.status === 'all') {
      // 获取所有审批记录（待审批 + 已审批）
      const [pendingRes, approvedRes] = await Promise.all([
        axios.get(`/purchase-requests/pending/approver/${currentUser.value.id}`),
        axios.get(`/purchase-requests/approved/approver/${currentUser.value.id}`)
      ])
      const pendingData = pendingRes.data.code === 200 ? pendingRes.data.data : []
      const approvedData = approvedRes.data.code === 200 ? approvedRes.data.data : []
      requests.value = [...pendingData, ...approvedData].sort((a, b) => 
        new Date(b.approvalDate || b.applicationDate) - new Date(a.approvalDate || a.applicationDate)
      )
      loading.value = false
      return
    } else {
      // 获取当前用户审批过的记录（已批准或已拒绝）
      url = `/purchase-requests/approved/approver/${currentUser.value.id}`
    }
    
    const response = await axios.get(url)
    if (response.data.code === 200) {
      // 如果是approved或rejected状态，过滤出对应状态的记录
      let data = response.data.data
      if (searchForm.value.status === 'approved' || searchForm.value.status === 'rejected') {
        data = data.filter(item => item.status === searchForm.value.status)
      }
      requests.value = data
    }
  } catch (error) {
    console.error('获取采购需求申请失败', error)
    ElMessage.error('获取采购需求申请失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  fetchRequests()
}

const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

const handleApprove = (row) => {
  approvalForm.value = {
    id: row.id,
    action: 'approve',
    approvalRemark: ''
  }
  approvalDialogTitle.value = '批准采购需求申请'
  approvalDialogVisible.value = true
}

const handleReject = (row) => {
  approvalForm.value = {
    id: row.id,
    action: 'reject',
    approvalRemark: ''
  }
  approvalDialogTitle.value = '拒绝采购需求申请'
  approvalDialogVisible.value = true
}

const handleApprovalSubmit = async () => {
  try {
    await approvalFormRef.value.validate()
    const url = approvalForm.value.action === 'approve' 
      ? `/purchase-requests/${approvalForm.value.id}/approve`
      : `/purchase-requests/${approvalForm.value.id}/reject`
    
    const response = await axios.post(url, {
      approverId: currentUser.value.id,
      approverName: currentUser.value.realName,
      approvalRemark: approvalForm.value.approvalRemark
    })
    
    if (response.data.code === 200) {
      ElMessage.success(approvalForm.value.action === 'approve' ? '批准成功' : '拒绝成功')
      approvalDialogVisible.value = false
      fetchRequests()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('[采购流程-步骤2] 操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleSearch = () => {
  fetchRequests()
}

const handleReset = () => {
  searchForm.value = {
    status: 'pending'
  }
  fetchRequests()
}

const handleBackToHome = () => {
  router.push('/home')
}

const getStatusType = (status) => {
  const typeMap = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已拒绝'
  }
  return textMap[status] || status
}

const formatPrice = (price) => {
  if (price == null) return '0.00'
  return parseFloat(price).toFixed(2)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  getCurrentUser()
  if (currentUser.value && currentUser.value.id) {
    fetchRequests()
  }
})
</script>

<style scoped>
.purchase-approval-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.content-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-select {
  width: 150px;
}
</style>
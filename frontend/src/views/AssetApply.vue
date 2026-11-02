<template>
  <ApplyPage
    page-class="asset-apply"
    :page-subtitle="pageSubtitle"
    :form-title="currentTypeLabel + '申请'"
    record-title="我的申请记录"
    :show-form="showApplyForm"
    :records="myApplications"
    :loading="loading"
    :record-columns="recordColumns"
    @form-submit="handleFormSubmit"
    @form-reset="handleFormReset"
  >
    <template #header-extra>
      <el-radio-group v-model="applicationType" class="type-switch" @change="handleTypeChange">
        <el-radio-button value="RECEIVE">资产领用</el-radio-button>
        <el-radio-button value="MAINTENANCE">资产维修</el-radio-button>
      </el-radio-group>
    </template>
    <template #form="{ submit, reset }">
      <ApplyForm
        :assetOptions="assetOptions"
        :applicationType="applicationType"
        @submit="submit"
        @reset="reset"
      />
    </template>
    
    <template #column-assetId="{ row }">
      <div class="cell-stack">
        <span class="cell-main">{{ getAssetName(row.assetId, allAssets) }}</span>
        <span class="cell-sub">{{ getAssetModel(row.assetId, allAssets) }}</span>
      </div>
    </template>
    
    <template #column-status="{ row }">
      <StatusTag :status="row.status" />
    </template>
    
    <template #column-createTime="{ row }">
      <TimeFormatter :time="row.createTime" />
    </template>
    
    <template #column-approvalDate="{ row }">
      <TimeFormatter :time="row.approvalDate" />
    </template>
  </ApplyPage>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/request'
import ApplyPage from '../components/business/ApplyPage.vue'
import ApplyForm from '../components/business/ApplyForm.vue'
import StatusTag from '../components/common/StatusTag.vue'
import TimeFormatter from '../components/common/TimeFormatter.vue'
import { getAssetName, getAssetModel } from '../utils/common.js'
import { useUserStore } from '../stores/userStore'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const myApplications = ref([])
const currentUser = ref(null)
const showApplyForm = ref(true)
const assetOptions = ref([])
const allAssets = ref([])
const applicationType = ref('RECEIVE')

const getToken = () => userStore.token

const typeLabels = {
  RECEIVE: '资产领用',
  MAINTENANCE: '资产维修'
}

const currentTypeLabel = computed(() => typeLabels[applicationType.value] || '资产申请')

const pageSubtitle = computed(() => applicationType.value === 'MAINTENANCE'
  ? '选择本人名下资产并填写故障情况，提交后进入维修审批'
  : '选择未被领用的资产并说明用途，提交后由部门审批')

// 记录表格列配置
const recordColumns = [
  { prop: 'id', label: '申请编号', width: '100' },
  { type: 'custom', prop: 'assetId', label: '资产信息', minWidth: '200' },
  { prop: 'applicationReason', label: '申请原因', minWidth: '200', showOverflowTooltip: true },
  { type: 'custom', prop: 'createTime', label: '申请时间', width: '180' },
  { type: 'custom', prop: 'status', label: '申请状态', width: '120' },
  { prop: 'approverName', label: '审批人', width: '120' },
  { type: 'custom', prop: 'approvalDate', label: '审批时间', width: '180' },
  { prop: 'approvalRemark', label: '审批备注', minWidth: '150', showOverflowTooltip: true }
]

const getCurrentUser = () => {
  if (userStore.user) {
    currentUser.value = userStore.user
  }
}

const fetchAssets = async () => {
  try {
    let response
    if (applicationType.value === 'RECEIVE') {
      // 领用申请：获取可领用的资产（未分配的资产）
      response = await axios.get('/assets/available')
    } else {
      // 其他申请类型：获取当前用户权限范围内的全部资产
      response = await axios.get('/assets/all')
    }
    if (response.data.code === 200) {
      const payload = response.data.data
      allAssets.value = Array.isArray(payload) ? payload : (payload.content || [])
      updateAssetOptions()
    }
  } catch (error) {
    console.error('获取资产失败:', error)
  }
}

const updateAssetOptions = () => {
  if (applicationType.value === 'RECEIVE') {
    // 领用申请：选择未被分配给用户的资产（userId为空或为null的资产）
    const filtered = allAssets.value.filter(asset =>
      !asset.userId || asset.userId === null || asset.userId === 0
    )
    assetOptions.value = filtered
  } else if (applicationType.value === 'MAINTENANCE') {
    // 维修申请：只能选择使用中或闲置的资产（仅限当前用户名下的资产，排除维修中的资产）
    if (!currentUser.value || !currentUser.value.id) {
      assetOptions.value = []
      return
    }
    assetOptions.value = allAssets.value.filter(asset => 
      ['using', 'idle'].includes(asset.status) &&
      asset.userId === currentUser.value.id
    )
  }
}

const handleTypeChange = () => {
  updateAssetOptions()
}

const fetchMyApplications = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/asset-applications/applicant/${currentUser.value.id}`)
    if (response.data.code === 200) {
      // 后端返回的是Page分页对象，实际数据在content属性中
      const data = response.data.data
      myApplications.value = data.content || data
    }
  } catch (error) {
    ElMessage.error('获取申请记录失败')
  } finally {
    loading.value = false
  }
}

const handleFormSubmit = async (formData) => {
  try {
    const requestData = {
      assetId: formData.assetId,
      applicationType: applicationType.value,
      applicantId: currentUser.value.id,
      applicantName: currentUser.value.realName || currentUser.value.username,
      departmentId: currentUser.value.departmentId,
      departmentName: currentUser.value.departmentName,
      applicationReason: formData.reason,
      maintenanceType: formData.maintenanceType,
      applicationDate: new Date().toISOString(),
      status: 'pending'
    }

    const response = await axios.post('/asset-applications', requestData)
    if (response.data.code === 200) {
      ElMessage.success('申请提交成功')
      fetchMyApplications()
    } else {
      ElMessage.error(response.data.message || '申请提交失败')
    }
  } catch (error) {
    ElMessage.error('申请提交失败')
  }
}

const handleFormReset = () => {
}

onMounted(() => {
  getCurrentUser()
  
  // 从 URL 参数读取申请类型，优先使用 URL 参数
  if (route.query.type && ['RECEIVE', 'MAINTENANCE'].includes(route.query.type.toUpperCase())) {
    applicationType.value = route.query.type.toUpperCase()
  }
  
  // 延迟执行，确保用户信息已加载
  setTimeout(() => {
    fetchAssets()
    // 确保用户数据加载完成后再获取申请记录
    if (currentUser.value && currentUser.value.id) {
      fetchMyApplications()
    }
  }, 100)
  
  if (route.query.from === 'dashboard') {
    showApplyForm.value = false
  }
})
</script>

<style scoped>
.type-switch {
  padding: 3px;
  background: #eaeef4;
  border-radius: 11px;
}

.type-switch :deep(.el-radio-button__inner) {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-500);
  background: transparent;
  border: none;
  border-radius: 8px;
  box-shadow: none !important;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease);
}

.type-switch :deep(.el-radio-button__inner:hover) {
  color: var(--eam-ink-800);
}

.type-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  font-weight: 600;
  color: var(--eam-ink-900);
  background: #fff;
  box-shadow: 0 1px 2px rgba(20, 24, 33, 0.08), 0 0 0 1px rgba(20, 24, 33, 0.05) !important;
}

.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cell-main {
  font-size: 13px;
  color: var(--eam-ink-800);
}

.cell-sub {
  font-size: 11.5px;
  color: var(--eam-ink-400);
}
</style>

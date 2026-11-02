<template>
  <ApplyPage
    page-class="asset-disposal"
    form-title="资产报废申请"
    record-title="我的报废记录"
    :records="myDisposals"
    :loading="loading"
    :record-columns="recordColumns"
    @form-submit="handleFormSubmit"
    @form-reset="handleFormReset"
  >
    <template #form>
      <DisposalForm
        :assetOptions="assetOptions"
        @submit="handleFormSubmit"
        @reset="handleFormReset"
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
  </ApplyPage>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/request'
import ApplyPage from '../components/business/ApplyPage.vue'
import DisposalForm from '../components/business/DisposalForm.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { getAssetName, getAssetModel } from '../utils/common.js'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()

const loading = ref(false)
const assetOptions = ref([])
const allAssets = ref([])
const myDisposals = ref([])
const currentUser = ref(null)

const getToken = () => userStore.token

// 记录表格列配置
const recordColumns = [
  { type: 'index', label: '序号', width: '60' },
  { type: 'custom', prop: 'assetId', label: '资产信息', minWidth: '200' },
  { prop: 'disposalType', label: '报废类型', width: '120' },
  { prop: 'estimatedValue', label: '估计价值', width: '120' },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { prop: 'createTime', label: '申请时间', width: '180' },
  { prop: 'applicationReason', label: '报废原因', minWidth: '200', showOverflowTooltip: true }
]

const getCurrentUser = () => {
  if (userStore.user) {
    currentUser.value = userStore.user
  }
}

const fetchAssetOptions = async () => {
  try {
    const response = await axios.get('/assets/all')
    if (response.data.code === 200) {
      allAssets.value = response.data.data
      // 只显示当前用户名下的资产
      assetOptions.value = response.data.data.filter(asset =>
        ['using', 'idle', 'maintenance'].includes(asset.status) &&
        asset.userId === currentUser.value.id
      )
    }
  } catch (error) {
    // 忽略错误
  }
}

const fetchMyDisposals = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/asset-applications/applicant/${currentUser.value.id}`)

    if (response.data.code === 200) {
      // 后端返回的是Page分页对象，实际数据在content属性中
      const data = response.data.data
      const applications = data.content || data
      myDisposals.value = applications.filter(item => item.applicationType === 'DISPOSAL')
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleFormSubmit = async (formData) => {
  try {
    const token = getToken()
    const disposalData = {
      assetId: formData.assetId,
      applicationType: 'DISPOSAL', // 资产报废申请
      applicantId: currentUser.value.id,
      applicantName: currentUser.value.realName || currentUser.value.username,
      departmentId: currentUser.value.departmentId,
      departmentName: currentUser.value.departmentName,
      applicationReason: formData.disposalReason,
      disposalType: formData.disposalType,
      estimatedValue: formData.estimatedValue,
      applicationDate: new Date().toISOString(),
      status: 'pending'
    }

    const response = await axios.post('/asset-applications', disposalData)

    if (response.data.code === 200) {
      ElMessage.success('申请提交成功')
      fetchMyDisposals()
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
  if (currentUser.value) {
    fetchAssetOptions()
    fetchMyDisposals()
  }
})
</script>

<style scoped>
</style>
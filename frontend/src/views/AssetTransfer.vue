<template>
  <ApplyPage
    page-class="asset-transfer"
    form-title="资产转移申请"
    record-title="我的转移记录"
    :records="myTransfers"
    :loading="loading"
    :record-columns="recordColumns"
    @form-submit="handleFormSubmit"
    @form-reset="handleFormReset"
  >
    <template #form>
      <TransferForm
        :assetOptions="assetOptions"
        :userOptions="userOptions"
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

    <template #column-transfereeDeptName="{ row }">
      {{ row.transfereeDeptName || row.departmentName || '-' }}
    </template>
  </ApplyPage>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/request'
import ApplyPage from '../components/business/ApplyPage.vue'
import TransferForm from '../components/business/TransferForm.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { getAssetName, getAssetModel } from '../utils/common.js'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()

const loading = ref(false)
const assetOptions = ref([])
const allAssets = ref([])
const userOptions = ref([])
const myTransfers = ref([])
const currentUser = ref(null)


// 记录表格列配置
const recordColumns = [
  { type: 'custom', prop: 'assetId', label: '资产信息', minWidth: '200' },
  { prop: 'transfereeName', label: '接收人', width: '120' },
  { type: 'custom', prop: 'transfereeDeptName', label: '接收部门', width: '150' },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { prop: 'createTime', label: '申请时间', width: '180' },
  { prop: 'transferReason', label: '转移原因', minWidth: '200', showOverflowTooltip: true }
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
      assetOptions.value = response.data.data.filter(asset =>
        asset.status === 'using' && asset.userId === currentUser.value.id
      )
    }
  } catch (error) {
    console.error('获取资产选项失败', error)
  }
}

const fetchUserOptions = async () => {
  try {
    const response = await axios.get('/users')
    if (response.data.code === 200) {
      // 过滤掉当前用户自己，不允许选择自己作为接收人
      userOptions.value = response.data.data.filter(user => 
        user.id !== currentUser.value.id
      )
    }
  } catch (error) {
    console.error('获取用户选项失败', error)
  }
}

const fetchMyTransfers = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/asset-applications/applicant/${currentUser.value.id}`)

    if (response.data.code === 200) {
      // 后端返回的是Page分页对象，实际数据在content属性中
      const data = response.data.data
      const applications = data.content || data
      myTransfers.value = applications.filter(item => item.applicationType === 'TRANSFER')
    }
  } catch (error) {
    console.error('获取数据失败', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleFormSubmit = async (formData) => {
  try {
    const transferData = {
      assetId: formData.assetId,
      applicationType: 'TRANSFER', // 资产转移申请
      applicantId: currentUser.value.id,
      applicantName: currentUser.value.realName || currentUser.value.username,
      departmentId: currentUser.value.departmentId,
      departmentName: currentUser.value.departmentName,
      transfereeId: formData.transfereeId,
      transfereeName: formData.transfereeName,
      transfereeDeptId: formData.transfereeDeptId,
      transfereeDeptName: formData.transfereeDeptName,
      transferReason: formData.transferReason,
      applicationDate: new Date().toISOString(),
      status: 'pending'
    }

    const response = await axios.post('/asset-applications', transferData)

    if (response.data.code === 200) {
      ElMessage.success('申请提交成功')
      fetchMyTransfers()
    } else {
      ElMessage.error(response.data.message || '申请提交失败')
    }
  } catch (error) {
    console.error('提交申请失败', error)
    ElMessage.error('申请提交失败')
  }
}

const handleFormReset = () => {}

onMounted(() => {
  getCurrentUser()
  if (currentUser.value) {
    fetchAssetOptions()
    fetchUserOptions()
    fetchMyTransfers()
  }
})
</script>

<style scoped>
</style>
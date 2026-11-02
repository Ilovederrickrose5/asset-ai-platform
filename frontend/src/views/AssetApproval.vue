<template>
  <ApprovalPage
    :page-title="pageTitle"
    page-class="asset"
    :pending-tab-label="pendingTabLabel"
    :approved-tab-label="approvedTabLabel"
    :pending-empty-text="pendingEmptyText"
    :approved-empty-text="approvedEmptyText"
    view-dialog-title="申请详情"
    @submit-approval="handleSubmitApproval"
    ref="approvalPageRef"
  >
    <!-- 待审批列 -->
    <template #pendingColumns>
      <el-table-column prop="id" label="申请编号" width="100" />
      <el-table-column label="申请人" width="150">
        <template #default="scope">
          <div class="cell-stack">
            <span class="cell-main">{{ scope.row.applicantName }}</span>
            <span class="cell-sub">{{ scope.row.departmentName || '未分配部门' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请资产" min-width="200">
          <template #default="scope">
            <div class="cell-stack">
              <span class="cell-main">{{ scope.row.assetName || getAssetName(scope.row.assetId, assetOptions) }}</span>
              <span class="cell-sub">{{ scope.row.model || getAssetModel(scope.row.assetId, assetOptions) }}</span>
            </div>
          </template>
        </el-table-column>
      <el-table-column label="申请原因" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.transferReason || scope.row.applicationReason || scope.row.repairReason || scope.row.disposalReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="applicationDate" label="申请时间" width="180">
        <template #default="scope">
          <TimeFormatter :time="scope.row.applicationDate" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="审批状态" width="170">
        <template #default="scope">
          <div class="cell-stack">
            <StatusTag :status="scope.row.status" :status-map="pendingStatusMap" />
            <span v-if="scope.row.status === 'leader_approved'" class="cell-sub cell-sub--success">
              领导已批准，可执行最终审批
            </span>
            <span v-else-if="scope.row.status === 'final_approval_created'" class="cell-sub">
              等待领导审批中
            </span>
          </div>
        </template>
      </el-table-column>
    </template>
    
    <!-- 已审批列 -->
    <template #approvedColumns>
      <el-table-column prop="id" label="申请编号" width="100" />
      <el-table-column label="申请人" width="150">
        <template #default="scope">
          <div class="cell-stack">
            <span class="cell-main">{{ scope.row.applicantName }}</span>
            <span class="cell-sub">{{ scope.row.departmentName || '未分配部门' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请资产" min-width="200">
          <template #default="scope">
            <div class="cell-stack">
              <span class="cell-main">{{ scope.row.assetName || getAssetName(scope.row.assetId, assetOptions) }}</span>
              <span class="cell-sub">{{ scope.row.model || getAssetModel(scope.row.assetId, assetOptions) }}</span>
            </div>
          </template>
        </el-table-column>
      <el-table-column label="申请原因" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.transferReason || scope.row.applicationReason || scope.row.repairReason || scope.row.disposalReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <StatusTag :status="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="approverName" label="审批人" width="120" />
      <el-table-column prop="approvalDate" label="审批时间" width="180">
        <template #default="scope">
          <TimeFormatter :time="scope.row.approvalDate" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link class="action-link" @click="approvalPageRef.handleView(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </template>

    <!-- 审批详情 -->
    <template #approvalDetails="{ item }">
      <el-form-item label="申请人">
        <span>{{ item?.applicantName }}</span>
      </el-form-item>
      <el-form-item label="申请资产">
        <span>{{ item?.assetName || getAssetName(item?.assetId, assetOptions) }}</span>
      </el-form-item>
      <el-form-item label="申请原因">
        <span>{{ item?.applicationReason }}</span>
      </el-form-item>
    </template>

    <!-- 查看详情 -->
    <template #viewDetails="{ item }">
      <el-descriptions-item label="申请编号">{{ item?.id }}</el-descriptions-item>
      <el-descriptions-item label="申请状态">
        <StatusTag :status="item?.status" />
      </el-descriptions-item>
      <el-descriptions-item label="申请人">{{ item?.applicantName }}</el-descriptions-item>
      <el-descriptions-item label="所属部门">{{ item?.departmentName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="申请资产" :span="2">
        {{ item?.assetName || getAssetName(item?.assetId, assetOptions) }} - {{ item?.model || getAssetModel(item?.assetId, assetOptions) }}
      </el-descriptions-item>
      <el-descriptions-item label="申请原因" :span="2">{{ item?.applicationReason }}</el-descriptions-item>
      <el-descriptions-item label="申请时间"><TimeFormatter :time="item?.applicationDate" /></el-descriptions-item>
      <el-descriptions-item label="审批人">{{ item?.approverName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审批时间"><TimeFormatter :time="item?.approvalDate" /></el-descriptions-item>
      <el-descriptions-item label="审批备注" :span="2">{{ item?.approvalRemark || '-' }}</el-descriptions-item>
    </template>
  </ApprovalPage>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '../utils/request'
import ApprovalPage from '../components/business/ApprovalPage.vue'
import StatusTag from '../components/common/StatusTag.vue'
import TimeFormatter from '../components/common/TimeFormatter.vue'
import { getAssetName, getAssetModel } from '../utils/common.js'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const approvalPageRef = ref(null)
const currentUser = ref(null)
const leaderStatus = ref(false)
const assetOptions = ref([])

const applicationType = computed(() => route.query.type || 'RECEIVE')

const typeConfig = {
  RECEIVE: {
    title: '资产领用审批',
    pendingLabel: '待审批资产申请',
    approvedLabel: '已审批资产申请',
    pendingEmpty: '暂无待审批的资产申请',
    approvedEmpty: '暂无已审批的资产申请'
  },
  TRANSFER: {
    title: '资产转移审批',
    pendingLabel: '待审批资产转移',
    approvedLabel: '已审批资产转移',
    pendingEmpty: '暂无待审批的资产转移',
    approvedEmpty: '暂无已审批的资产转移'
  },
  REPAIR: {
    title: '资产维修审批',
    pendingLabel: '待审批资产维修',
    approvedLabel: '已审批资产维修',
    pendingEmpty: '暂无待审批的资产维修',
    approvedEmpty: '暂无已审批的资产维修'
  },
  MAINTENANCE: {
    title: '资产维修审批',
    pendingLabel: '待审批资产维修',
    approvedLabel: '已审批资产维修',
    pendingEmpty: '暂无待审批的资产维修',
    approvedEmpty: '暂无已审批的资产维修'
  },
  DISPOSAL: {
    title: '资产报废审批',
    pendingLabel: '待审批资产报废',
    approvedLabel: '已审批资产报废',
    pendingEmpty: '暂无待审批的资产报废',
    approvedEmpty: '暂无已审批的资产报废'
  }
}

const pageTitle = computed(() => typeConfig[applicationType.value]?.title || '资产申请审批')
const pendingTabLabel = computed(() => typeConfig[applicationType.value]?.pendingLabel || '待审批资产申请')
const approvedTabLabel = computed(() => typeConfig[applicationType.value]?.approvedLabel || '已审批资产申请')
const pendingEmptyText = computed(() => typeConfig[applicationType.value]?.pendingEmpty || '暂无待审批的资产申请')
const approvedEmptyText = computed(() => typeConfig[applicationType.value]?.approvedEmpty || '暂无已审批的资产申请')

const getToken = () => userStore.token

const getCurrentUser = () => {
  if (userStore.user) {
    currentUser.value = userStore.user
    leaderStatus.value = currentUser.value?.roles?.some(role => role.toLowerCase().includes('leader')) || false
  }
}

const fetchPendingApplications = async () => {
  approvalPageRef.value.setLoading(true)
  try {
    // 构建请求URL
    // 修复角色判断：支持多种角色格式（admin, ROLE_admin, leader, ROLE_leader等）
    const isAdmin = currentUser.value?.roles?.some(role => {
      const roleLower = role.toLowerCase()
      return roleLower === 'admin' || roleLower === 'role_admin'
    }) || false
    leaderStatus.value = currentUser.value?.roles?.some(role => role.toLowerCase().includes('leader')) || false
    const deptId = currentUser.value?.departmentId
    
    let pendingUrl = ''
    let pendingLeaderUrl = ''
    let finalApprovalCreatedUrl = ''
    let leaderApprovedUrl = ''
    
    // 非系统管理员只能查看本部门的申请
    if (currentUser.value && !isAdmin && deptId) {
      pendingUrl = `/asset-applications/status/pending/department/${deptId}`
      // 资产管理员需要看到已创建终审申请但领导还没批准的记录
      finalApprovalCreatedUrl = `/asset-applications/status/final_approval_created/department/${deptId}`
      // 资产管理员需要看到领导已批准、等待自己最终批准的记录
      leaderApprovedUrl = `/asset-applications/status/leader_approved/department/${deptId}`
      if (leaderStatus.value) {  // 修复：使用 .value 访问 ref
        // 领导还需要查看等待领导审批的申请
        pendingLeaderUrl = `/asset-applications/status/pending_leader/department/${deptId}`
      }
    } else {
      pendingUrl = '/asset-applications/status/pending'
      // 系统管理员也需要看到已创建终审申请的记录
      finalApprovalCreatedUrl = '/asset-applications/status/final_approval_created'
      // 系统管理员也需要看到领导已批准的记录
      leaderApprovedUrl = '/asset-applications/status/leader_approved'
      if (leaderStatus.value) {  // 修复：使用 .value 访问 ref
        // 系统管理员或领导查看所有等待领导审批的申请
        pendingLeaderUrl = '/asset-applications/status/pending_leader'
      }
    }
    
    // 发送请求
    const requests = [axios.get(pendingUrl)]
    if (pendingLeaderUrl) {
      requests.push(axios.get(pendingLeaderUrl))
    }
    if (finalApprovalCreatedUrl) {
      requests.push(axios.get(finalApprovalCreatedUrl))
    }
    if (leaderApprovedUrl) {
      requests.push(axios.get(leaderApprovedUrl))
    }
    
    const responses = await Promise.all(requests)
    
    // 合并结果
    let applications = []
    responses.forEach((response) => {
      if (response.data.code === 200) {
        applications = applications.concat(response.data.data)
      }
    })
    
    // 过滤逻辑
    if (leaderStatus.value) {
      // 领导只能看到终审申请（状态为 pending_leader 或来自资产管理员的申请）
      applications = applications.filter(item => {
        return item.status === 'pending_leader' || item.applicationReason?.includes('资产报废终审申请')
      })
    } else if (!isAdmin) {
      // 非管理员且非领导，过滤掉当前用户作为申请人的申请
      applications = applications.filter(item => {
        return item.applicantId !== currentUser.value.id
      })
    }
    // 系统管理员不过滤，可以看到所有申请包括自己提交的
    
    // 根据类型过滤
    if (applicationType.value) {
      applications = applications.filter(item => item.applicationType === applicationType.value)
    }
    
    approvalPageRef.value.setPendingItems(applications)
  } catch (error) {
    ElMessage.error('获取待审批申请列表失败')
  } finally {
    approvalPageRef.value.setLoading(false)
  }
}

const fetchApprovedApplications = async () => {
  approvalPageRef.value.setLoading(true)
  try {
    let url = '/asset-applications'
    
    // 检查用户是否为系统管理员（支持多种角色格式）
    const isAdmin = currentUser.value?.roles?.some(role => {
      const roleLower = role.toLowerCase()
      return roleLower === 'admin' || roleLower === 'role_admin'
    }) || false
    const deptId = currentUser.value?.departmentId
    
    // 非系统管理员只能查看本部门的申请
    if (currentUser.value && !isAdmin && deptId) {
      url = `/asset-applications/department/${deptId}`
    }
    
    const response = await axios.get(url)
    
    if (response.data.code === 200) {
      // 后端返回的是Page分页对象，实际数据在content属性中
      const data = response.data.data
      const applicationsData = data.content || data
      let applications = applicationsData.filter(item => 
        item.status === 'approved' || item.status === 'rejected'
      )
      // 根据类型过滤
      if (applicationType.value) {
        applications = applications.filter(item => item.applicationType === applicationType.value)
      }
      approvalPageRef.value.setApprovedItems(applications)
    }
  } catch (error) {
    ElMessage.error('获取已审批申请列表失败')
  } finally {
    approvalPageRef.value.setLoading(false)
  }
}

const fetchAssets = async () => {
  try {
    const response = await axios.get('/assets/all')
    if (response.data.code === 200) {
      assetOptions.value = response.data.data
    }
  } catch (error) {
    // 忽略错误
  }
}

const handleSubmitApproval = async ({ formData, item, type }) => {
  approvalPageRef.value.setLoading(true)
  try {
    // 审批权限由后端根据当前登录用户角色判定，前端不再做角色分支
    const url = type === 'approve'
      ? `/asset-applications/${item.id}/approve`
      : `/asset-applications/${item.id}/reject`

    const approvalData = {
      approverId: currentUser.value.id,
      approverName: currentUser.value.realName || currentUser.value.username,
      approvalRemark: formData.approvalRemark
    }

    const response = await axios.post(url, approvalData)

    if (response.data.code === 200) {
      ElMessage.success(type === 'approve' ? '批准成功' : '拒绝成功')
      approvalPageRef.value.approvalDialogVisible = false
      fetchPendingApplications()
      fetchApprovedApplications()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    approvalPageRef.value.setLoading(false)
  }
}

const pendingStatusMap = {
  pending: { text: '待处理', type: 'warning' },
  final_approval_created: { text: '已提交终审', type: 'info' },
  pending_leader: { text: '等待领导审批', type: 'warning' },
  leader_approved: { text: '领导已批准', type: 'primary' },
  approved: { text: '已完成', type: 'success' },
  rejected: { text: '已拒绝', type: 'danger' }
}

onMounted(() => {
  getCurrentUser()
  fetchAssets()
  fetchPendingApplications()
  fetchApprovedApplications()
})
</script>

<style scoped>
.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cell-main {
  font-size: 13px;
  color: var(--eam-ink-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-sub {
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--eam-ink-400);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-sub--success {
  color: #34634f;
}

.action-link {
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-primary-deep);
}
</style>
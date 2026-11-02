<template>
  <div class="eam-page" :class="`${pageClass}-approval`">
    <PageHeader
      :title="pageTitle"
      :subtitle="pageSubtitle"
      eyebrow="APPROVAL"
    >
      <template #actions>
        <slot name="header-extra" />
      </template>
    </PageHeader>

    <div class="eam-page-body">
      <section class="eam-panel">
        <div class="eam-panel__head">
          <div class="eam-panel__title">
            <span class="eam-panel__icon"><el-icon><Stamp /></el-icon></span>
            <div>
              <strong>审批队列</strong>
              <span>按状态分组，优先处理待办事项</span>
            </div>
          </div>
          <span v-if="pendingItems.length" class="eam-panel__count">
            {{ pendingItems.length }} 条待处理
          </span>
        </div>

        <el-tabs v-model="activeTab" class="approval-tabs">
          <el-tab-pane name="pending">
            <template #label>
              <span class="tab-label">{{ pendingTabLabel }}</span>
              <span v-if="pendingItems.length" class="tab-count">{{ pendingItems.length }}</span>
            </template>

            <div class="tab-body">
              <ApprovalTable
                v-if="pendingItems.length"
                :records="pendingItems"
                :loading="loading"
                @approve="handleApprove"
                @reject="handleReject"
                @view="handleView"
              >
                <template #columns>
                  <slot name="pendingColumns" />
                </template>
                <template #actionButtons="{ row }">
                  <slot name="actionButtons" :row="row" />
                </template>
              </ApprovalTable>

              <div v-else-if="!loading" class="empty-state">
                <span class="empty-state__icon"><el-icon><Tickets /></el-icon></span>
                <strong>{{ pendingEmptyText }}</strong>
                <span>新的申请提交后会出现在这里</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane name="approved">
            <template #label>
              <span class="tab-label">{{ approvedTabLabel }}</span>
            </template>

            <div class="tab-body">
              <el-table v-if="approvedItems.length" :data="approvedItems" style="width: 100%">
                <slot name="approvedColumns" />
              </el-table>

              <div v-else-if="!loading" class="empty-state">
                <span class="empty-state__icon"><el-icon><CircleCheck /></el-icon></span>
                <strong>{{ approvedEmptyText }}</strong>
                <span>完成审批的申请会归档在这里</span>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>

    <!-- 审批对话框 -->
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :approvalType="approvalType"
      :loading="approvalLoading"
      @submit="submitApproval"
    >
      <template #details>
        <slot name="approvalDetails" :item="currentItem" />
      </template>
    </ApprovalDialog>

    <!-- 查看详情对话框 -->
    <ViewDialog
      v-model:visible="viewDialogVisible"
      :title="viewDialogTitle"
    >
      <slot name="viewDetails" :item="currentItem" />
    </ViewDialog>

    <!-- 额外对话框（如完成报废） -->
    <slot name="extraDialogs" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import PageHeader from '../common/PageHeader.vue'
import ApprovalTable from './ApprovalTable.vue'
import ApprovalDialog from './ApprovalDialog.vue'
import ViewDialog from './ViewDialog.vue'
import { Stamp, Tickets, CircleCheck } from '@element-plus/icons-vue'

const props = defineProps({
  pageTitle: {
    type: String,
    required: true
  },
  pageClass: {
    type: String,
    required: true
  },
  pageSubtitle: {
    type: String,
    default: '审批结果会同步到申请人与资产状态，操作均有记录'
  },
  pendingTabLabel: {
    type: String,
    default: '待审批'
  },
  approvedTabLabel: {
    type: String,
    default: '已审批'
  },
  pendingEmptyText: {
    type: String,
    default: '暂无待审批的申请'
  },
  approvedEmptyText: {
    type: String,
    default: '暂无已审批的申请'
  },
  viewDialogTitle: {
    type: String,
    default: '申请详情'
  }
})

const emit = defineEmits(['approve', 'reject', 'view', 'submitApproval'])

// 状态管理
const loading = ref(false)
const approvalLoading = ref(false)
const approvalDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const approvalType = ref('approve')
const activeTab = ref('pending')
const currentItem = ref({})
const pendingItems = ref([])
const approvedItems = ref([])

// 方法
const handleApprove = (item) => {
  currentItem.value = item
  approvalType.value = 'approve'
  approvalDialogVisible.value = true
}

const handleReject = (item) => {
  currentItem.value = item
  approvalType.value = 'reject'
  approvalDialogVisible.value = true
}

const handleView = (item) => {
  currentItem.value = item
  viewDialogVisible.value = true
}

const submitApproval = async (formData) => {
  if (!currentItem.value) return
  emit('submitApproval', { formData, item: currentItem.value, type: approvalType.value })
}

// 设置加载状态
const setLoading = (value) => {
  loading.value = value
}

// 设置待审批项目
const setPendingItems = (items) => {
  pendingItems.value = items
}

// 设置已审批项目
const setApprovedItems = (items) => {
  approvedItems.value = items
}

// 暴露给父组件的方法
defineExpose({
  setLoading,
  setPendingItems,
  setApprovedItems,
  currentItem,
  approvalDialogVisible,
  viewDialogVisible,
  handleApprove,
  handleReject,
  handleView,
  submitApproval
})
</script>

<style scoped>
.approval-tabs {
  padding: 0;
}

.approval-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 22px;
  border-bottom: 1px solid var(--eam-line-soft);
}

.approval-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.approval-tabs :deep(.el-tabs__item) {
  height: 48px;
  padding: 0 4px;
  margin-right: 24px;
  font-size: 13px;
  color: var(--eam-ink-400);
  transition: color var(--eam-duration-fast) var(--eam-ease);
}

.approval-tabs :deep(.el-tabs__item:hover) {
  color: var(--eam-ink-700);
}

.approval-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: var(--eam-ink-900);
}

.approval-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  border-radius: 2px;
  background: var(--eam-primary);
}

.tab-label {
  letter-spacing: 0.01em;
}

.tab-count {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: #8a6430;
  background: var(--eam-warning-soft);
  border-radius: var(--eam-radius-pill);
}

.tab-body {
  padding: 6px 22px 18px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 52px 0 46px;
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin-bottom: 4px;
  font-size: 20px;
  color: var(--eam-ink-300);
  background: #f3f5f8;
  border: 1px solid var(--eam-line-soft);
  border-radius: 14px;
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
</style>

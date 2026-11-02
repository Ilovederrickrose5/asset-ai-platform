<template>
  <div class="eam-page" :class="`${pageClass}-apply`">
    <PageHeader
      :title="pageTitle || formTitle"
      :subtitle="pageSubtitle"
      :eyebrow="eyebrow"
      :show-back="showBackButton"
    >
      <template #actions>
        <slot name="header-extra" />
      </template>
    </PageHeader>

    <div class="eam-page-body">
      <section v-if="showForm" class="eam-panel">
        <div class="eam-panel__head">
          <div class="eam-panel__title">
            <span class="eam-panel__icon"><el-icon><EditPen /></el-icon></span>
            <div>
              <strong>申请信息</strong>
              <span>{{ formHint }}</span>
            </div>
          </div>
          <slot name="form-actions" />
        </div>

        <div class="eam-panel__body form-body">
          <slot name="form" :submit="handleFormSubmit" :reset="handleFormReset">
            <component
              :is="formComponent"
              v-if="formComponent"
              v-bind="formProps"
              @submit="handleFormSubmit"
              @reset="handleFormReset"
            />
          </slot>
        </div>
      </section>

      <section class="eam-panel">
        <div class="eam-panel__head">
          <div class="eam-panel__title">
            <span class="eam-panel__icon"><el-icon><Tickets /></el-icon></span>
            <div>
              <strong>{{ recordTitle }}</strong>
              <span>审批进度会在这里同步更新</span>
            </div>
          </div>
          <span class="eam-panel__count">{{ records.length }} 条</span>
        </div>

        <div class="eam-panel__body table-body">
          <DataTable
            :data="records"
            :columns="recordColumns"
            :loading="loading"
            :show-index="showIndex"
            :show-pagination="showPagination"
            :total="total"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :show-edit-button="false"
            :show-delete-button="false"
            :empty-text="recordEmptyText"
            :empty-hint="recordEmptyHint"
            @page-change="handlePageChange"
          >
            <template v-for="column in customColumns" :key="column" #[`column-${column}`]="{ row, value }">
              <slot :name="`column-${column}`" :row="row" :value="value" />
            </template>

            <template #actions="{ row }" v-if="showRecordActions">
              <slot name="record-actions" :row="row" />
            </template>
          </DataTable>
        </div>
      </section>
    </div>

    <slot name="dialogs" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import PageHeader from '../common/PageHeader.vue'
import DataTable from '../common/DataTable.vue'
import { EditPen, Tickets } from '@element-plus/icons-vue'

const props = defineProps({
  // 页面基础配置
  pageClass: {
    type: String,
    default: 'asset'
  },
  pageTitle: {
    type: String,
    default: ''
  },
  pageSubtitle: {
    type: String,
    default: '提交申请后由部门资产管理员或领导审批，进度可随时查看'
  },
  eyebrow: {
    type: String,
    default: 'APPLICATION'
  },
  formHint: {
    type: String,
    default: '填写资产与申请原因，提交后进入审批流程'
  },
  recordEmptyText: {
    type: String,
    default: '暂无申请记录'
  },
  recordEmptyHint: {
    type: String,
    default: '提交申请后，审批进度会显示在这里'
  },

  // 表单配置
  showForm: {
    type: Boolean,
    default: true
  },
  formTitle: {
    type: String,
    default: ''
  },
  formComponent: {
    type: Object,
    default: null
  },
  formProps: {
    type: Object,
    default: () => ({})
  },

  // 记录配置
  recordTitle: {
    type: String,
    default: '我的申请记录'
  },
  records: {
    type: Array,
    default: () => []
  },
  recordColumns: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: true
  },
  showRecordActions: {
    type: Boolean,
    default: false
  },

  // 分页配置
  showPagination: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  },

  // 其他配置
  showBackButton: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits([
  'form-submit',
  'form-reset',
  'page-change'
])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(props.pageSize)

// 计算自定义列
const customColumns = computed(() => {
  return props.recordColumns
    .filter(col => col.type === 'custom' || col.slot)
    .map(col => col.prop)
})

// 方法
const handleFormSubmit = (formData) => {
  emit('form-submit', formData)
}

const handleFormReset = () => {
  emit('form-reset')
}

const handlePageChange = ({ pageSize: size, currentPage: page }) => {
  emit('page-change', { pageSize: size, currentPage: page })
}

// 暴露方法给父组件
defineExpose({
  currentPage,
  pageSize
})
</script>

<style scoped>
.form-body {
  max-width: 880px;
}

.table-body {
  padding-top: 6px;
}
</style>

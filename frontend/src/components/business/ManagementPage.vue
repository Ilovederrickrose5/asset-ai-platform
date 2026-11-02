<template>
  <div :class="`${pageClass}-container`">
    <el-card :class="`${pageClass}-card`">
      <template #header>
        <div class="card-header">
          <el-button 
            v-if="showBackButton" 
            type="info" 
            @click="goBack" 
            :icon="ArrowLeft"
          >
            返回首页
          </el-button>
          <span class="title">{{ title }}</span>
          <el-button 
            v-if="showAddButton && hasPermission" 
            type="primary" 
            @click="handleAdd" 
            :icon="Plus"
          >
            {{ addButtonText }}
          </el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <SearchForm
        v-if="searchFields && searchFields.length > 0"
        :fields="searchFields"
        v-model="searchForm"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template v-for="slot in searchSlots" :key="slot" #[slot]="{ field, form }">
          <slot :name="`search-${slot}`" :field="field" :form="form" />
        </template>
      </SearchForm>

      <!-- 数据表格 -->
      <div class="table-wrapper">
        <div class="table-container">
          <DataTable
          :data="filteredData"
          :columns="tableColumns"
          :loading="loading"
          :show-selection="showSelection"
          :show-index="showIndex"
          :show-edit-button="showEditButton && hasPermission"
          :show-delete-button="showDeleteButton && hasPermission"
          :show-pagination="showPagination"
          :total="total"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          @selection-change="handleSelectionChange"
          @edit="handleEdit"
          @delete="handleDelete"
          @page-change="handlePageChange"
        >
          <template #actions="{ row }">
            <slot name="actions" :row="row">
              <el-button 
                v-if="showEditButton && hasPermission" 
                type="primary" 
                size="small" 
                @click="handleEdit(row)" 
                :icon="Edit"
              >
                编辑
              </el-button>
              <el-button 
                v-if="showDeleteButton && hasPermission" 
                type="danger" 
                size="small" 
                @click="handleDelete(row)" 
                :icon="Delete"
              >
                删除
              </el-button>
            </slot>
          </template>
          
          <template v-for="column in customColumns" :key="column" #[`column-${column}`]="{ row, value }">
            <slot :name="`column-${column}`" :row="row" :value="value" />
          </template>
          </DataTable>
        </div>
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <FormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="dialogWidth"
      :fields="formFields"
      :form-data="formData"
      :form-rules="formRules"
      :label-width="formLabelWidth"
      :is-edit="isEdit"
      :destroy-on-close="destroyOnClose"
      @submit="handleSubmit"
      @cancel="handleCancel"
      @closed="handleDialogClosed"
      ref="formDialogRef"
    >
      <template v-for="field in customFormFields" :key="field.prop" #[`field-${field.kebabProp}`]="{ form, isEdit }">
        <slot :name="`form-${field.kebabProp}`" :field="field" :form="form" :isEdit="isEdit" />
    </template>
      
      <template #footer="{ form, isEdit, submitting }">
        <slot name="form-footer" :form="form" :isEdit="isEdit" :submitting="submitting">
          <el-button @click="handleCancel" :disabled="submitting">取消</el-button>
          <el-button type="primary" @click="handleSubmit(form)" :loading="submitting">
            {{ submitting ? '提交中...' : '确定' }}
          </el-button>
        </slot>
      </template>
    </FormDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Edit, Delete, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '../common/SearchForm.vue'
import DataTable from '../common/DataTable.vue'
import FormDialog from '../common/FormDialog.vue'

const props = defineProps({
  // 页面基础配置
  title: {
    type: String,
    required: true
  },
  pageClass: {
    type: String,
    default: 'management'
  },
  
  // 权限配置
  permission: {
    type: String,
    default: ''
  },
  hasPermission: {
    type: Boolean,
    default: true
  },
  
  // 按钮配置
  showBackButton: {
    type: Boolean,
    default: true
  },
  showAddButton: {
    type: Boolean,
    default: true
  },
  addButtonText: {
    type: String,
    default: '新增'
  },
  showEditButton: {
    type: Boolean,
    default: true
  },
  showDeleteButton: {
    type: Boolean,
    default: true
  },
  
  // 数据配置
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  
  // 搜索配置
  searchFields: {
    type: Array,
    default: () => []
  },
  
  // 表格配置
  columns: {
    type: Array,
    required: true
  },
  showSelection: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: true
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
  
  // 表单配置
  formFields: {
    type: Array,
    default: () => []
  },
  formRules: {
    type: Object,
    default: () => ({})
  },
  formLabelWidth: {
    type: String,
    default: '100px'
  },
  dialogWidth: {
    type: String,
    default: '500px'
  },
  destroyOnClose: {
    type: Boolean,
    default: false
  },
  
  // 其他配置
  deleteConfirmMessage: {
    type: String,
    default: '确定要删除这条记录吗？'
  }
})

const emit = defineEmits([
  'add',
  'edit',
  'delete',
  'submit',
  'search',
  'reset',
  'page-change',
  'selection-change'
])

const router = useRouter()

// 搜索相关
const searchForm = reactive({})
const searchSlots = computed(() => {
  return props.searchFields
    .filter(field => field.type === 'custom')
    .map(field => field.prop)
})

// 表格相关
const currentPage = ref(1)
const pageSize = ref(props.pageSize)

// 计算过滤后的数据
const filteredData = computed(() => {
  if (!props.searchFields || props.searchFields.length === 0) {
    return props.data
  }
  
  return props.data.filter(item => {
    return Object.keys(searchForm).every(key => {
      const value = searchForm[key]
      if (!value && value !== 0) return true
      
      const itemValue = item[key]
      if (typeof itemValue === 'string') {
        return itemValue.toLowerCase().includes(String(value).toLowerCase())
      }
      return itemValue === value
    })
  })
})

// 处理表格列配置
const tableColumns = computed(() => {
  return props.columns.map(col => {
    if (typeof col === 'string') {
      return { prop: col, label: col }
    }
    return col
  })
})

const customColumns = computed(() => {
  return props.columns
    .filter(col => col.type === 'custom' || col.slot)
    .map(col => col.prop)
})

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const formDialogRef = ref(null)

// 表单数据
const formData = reactive({})

// 初始化表单数据
const initFormData = () => {
  props.formFields.forEach(field => {
    formData[field.prop] = field.defaultValue !== undefined ? field.defaultValue : ''
  })
}

// 计算属性
const dialogTitle = computed(() => {
  return isEdit.value ? `编辑${props.title.replace('管理', '')}` : `新增${props.title.replace('管理', '')}`
})

const customFormFields = computed(() => {
  return props.formFields
    .filter(field => field.type === 'custom')
    .map(field => ({
      ...field,
      kebabProp: field.prop.toLowerCase()
    }))
})

// 方法
const goBack = () => {
  router.push('/dashboard')
}

const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  initFormData()
  dialogVisible.value = true
  emit('add')
}

const handleEdit = (row) => {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(formData, row)
  // 将 deptId 转换为 departmentId，匹配前端表单字段
  if (row.deptId !== undefined) {
    formData.departmentId = row.deptId
  }
  dialogVisible.value = true
  emit('edit', row)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(props.deleteConfirmMessage, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('delete', row)
  }).catch(() => {})
}

const handleSubmit = async (form) => {
  emit('submit', { ...form, isEdit: isEdit.value, id: currentId.value })
}

const handleCancel = () => {
  dialogVisible.value = false
}

const handleDialogClosed = () => {
  formDialogRef.value?.resetForm()
}

const handleSearch = (form) => {
  currentPage.value = 1
  emit('search', form)
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    delete searchForm[key]
  })
  currentPage.value = 1
  emit('reset')
}

const handlePageChange = ({ pageSize: size, currentPage: page }) => {
  emit('page-change', { pageSize: size, currentPage: page })
}

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

// 暴露方法给父组件
defineExpose({
  dialogVisible,
  isEdit,
  currentId,
  formData,
  searchForm,
  currentPage,
  pageSize,
  resetForm: () => formDialogRef.value?.resetForm(),
  setSubmitting: (value) => formDialogRef.value?.setSubmitting(value),
  closeDialog: () => { dialogVisible.value = false },
  getFormData: () => formData,
  setFormData: (data) => Object.assign(formData, data),
  handleEdit
})

// 初始化
initFormData()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 10;
  flex-wrap: wrap;
  gap: 10px;
}

.card-header .title {
  font-size: 16px;
  font-weight: bold;
  flex-shrink: 0;
}

.card-header .el-button {
  flex-shrink: 0;
}

.table-wrapper {
  overflow-x: auto;
  margin-top: 20px;
  border-radius: 4px;
}

.table-container {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  min-width: 100%;
}

/* 滚动条样式 */
.table-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-height: 768px) {
  .table-container {
    max-height: calc(100vh - 180px);
  }
}

@media (max-height: 600px) {
  .table-container {
    max-height: calc(100vh - 160px);
  }
}

@media (max-width: 768px) {
  .card-header {
    padding: 10px 15px;
  }
  
  .card-header .title {
    font-size: 14px;
  }
  
  .table-wrapper {
    margin-top: 15px;
  }
}

@media (max-width: 480px) {
  .card-header {
    padding: 8px 10px;
  }
  
  .card-header .title {
    font-size: 13px;
  }
}
</style>

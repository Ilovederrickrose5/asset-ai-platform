<template>
  <div class="data-table-wrapper">
    <template v-if="!loading && data.length === 0">
      <div class="empty-wrap">
        <span class="empty-icon"><el-icon><Files /></el-icon></span>
        <p class="empty-text">{{ emptyText }}</p>
        <p v-if="emptyHint" class="empty-hint">{{ emptyHint }}</p>
      </div>
    </template>
    <el-table
      v-else
      class="eam-data-table"
      :data="data"
      v-loading="loading"
      :border="border"
      :height="height"
      :max-height="maxHeight"
      @selection-change="handleSelectionChange"
    >
      <!-- 选择列 -->
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="55"
        fixed="left"
      />
      
      <!-- 序号列 -->
      <el-table-column
        v-if="showIndex"
        type="index"
        label="序号"
        width="60"
        :index="indexMethod"
        fixed="left"
      />
      
      <!-- 动态列 -->
      <template v-for="column in columns" :key="column.prop || column.type">
        <!-- 操作列 -->
        <el-table-column
          v-if="column.type === 'actions'"
          :label="column.label || '操作'"
          :width="column.width || '200'"
          :fixed="column.fixed || 'right'"
        >
          <template #default="scope">
            <slot name="actions" :row="scope.row" :index="scope.$index">
              <el-button
                v-if="showEditButton"
                type="primary"
                size="small"
                :icon="Edit"
                @click="handleEdit(scope.row)"
              >编辑</el-button>
              <el-button
                v-if="showDeleteButton"
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(scope.row)"
              >删除</el-button>
            </slot>
          </template>
        </el-table-column>
        
        <!-- 状态列 -->
        <el-table-column
          v-else-if="column.type === 'status'"
          :prop="column.prop"
          :label="column.label || '状态'"
          :width="column.width || '100'"
        >
          <template #default="scope">
            <slot name="status" :row="scope.row" :value="scope.row[column.prop]">
              <el-tag :type="getStatusType(scope.row[column.prop], column.statusMap)">
                {{ getStatusText(scope.row[column.prop], column.statusMap) }}
              </el-tag>
            </slot>
          </template>
        </el-table-column>
        
        <!-- 时间列 -->
        <el-table-column
          v-else-if="column.type === 'time'"
          :prop="column.prop"
          :label="column.label"
          :width="column.width || '180'"
        >
          <template #default="scope">
            <slot name="time" :row="scope.row" :value="scope.row[column.prop]">
              {{ formatTime(scope.row[column.prop]) }}
            </slot>
          </template>
        </el-table-column>
        
        <!-- 自定义列 -->
        <el-table-column
          v-else-if="column.type === 'custom'"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
          :show-overflow-tooltip="column.showOverflowTooltip"
          :fixed="column.fixed"
        >
          <template #default="scope">
            <slot :name="`column-${column.prop}`" :row="scope.row" :value="scope.row[column.prop]" :index="scope.$index">
              {{ scope.row[column.prop] }}
            </slot>
          </template>
        </el-table-column>
        
        <!-- 普通列 -->
        <el-table-column
          v-else
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
          :show-overflow-tooltip="column.showOverflowTooltip"
          :fixed="column.fixed"
        />
      </template>
    </el-table>
    
    <!-- 分页 -->
    <div v-if="showPagination" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        :layout="paginationLayout"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Edit, Delete, Files } from '@element-plus/icons-vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  border: {
    type: Boolean,
    default: false
  },
  height: {
    type: [String, Number],
    default: null
  },
  maxHeight: {
    type: [String, Number],
    default: null
  },
  showSelection: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: true
  },
  showEditButton: {
    type: Boolean,
    default: true
  },
  showDeleteButton: {
    type: Boolean,
    default: true
  },
  showPagination: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    default: 0
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  },
  paginationLayout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  emptyText: {
    type: String,
    default: '暂无记录'
  },
  emptyHint: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update:currentPage',
  'update:pageSize',
  'selection-change',
  'edit',
  'delete',
  'page-change'
])

const currentPage = computed({
  get: () => props.currentPage,
  set: (val) => emit('update:currentPage', val)
})

const pageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val)
})

const indexMethod = (index) => {
  if (props.showPagination) {
    return (props.currentPage - 1) * props.pageSize + index + 1
  }
  return index + 1
}

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleEdit = (row) => {
  emit('edit', row)
}

const handleDelete = (row) => {
  emit('delete', row)
}

const handleSizeChange = (val) => {
  emit('page-change', { pageSize: val, currentPage: 1 })
}

const handleCurrentChange = (val) => {
  emit('page-change', { pageSize: props.pageSize, currentPage: val })
}

const getStatusText = (status, statusMap) => {
  if (!statusMap) {
    const defaultMap = {
      1: '启用',
      0: '禁用',
      true: '启用',
      false: '禁用'
    }
    return defaultMap[status] || status
  }
  return statusMap[status]?.text || status
}

const getStatusType = (status, statusMap) => {
  if (!statusMap) {
    const defaultMap = {
      1: 'success',
      0: 'danger',
      true: 'success',
      false: 'danger'
    }
    return defaultMap[status] || 'info'
  }
  return statusMap[status]?.type || 'info'
}

const formatTime = (time, fallback = '-') => {
  if (!time) return fallback
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.data-table-wrapper {
  width: 100%;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 40px 0;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-bottom: 4px;
  font-size: 19px;
  color: var(--eam-ink-300);
  background: #f3f5f8;
  border: 1px solid var(--eam-line-soft);
  border-radius: 14px;
}

.empty-text {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-500);
}

.empty-hint {
  margin: 0;
  font-size: 12px;
  color: var(--eam-ink-300);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
  padding: 16px 4px 2px;
  border-top: 1px solid var(--eam-line-soft);
}

.data-table-wrapper :deep(.el-table__row) {
  height: 56px;
}

.data-table-wrapper :deep(.el-table .cell) {
  font-variant-numeric: tabular-nums;
}
</style>

<template>
  <div>
    <el-table :data="records" style="width: 100%" v-loading="loading">
      <slot name="columns"></slot>
    </el-table>
    
    <el-pagination
      v-if="records.length > 0 && showPagination"
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="handleCurrentChange"
      class="mt-4"
    />
    
    <div v-else-if="records.length === 0" class="empty-state">
      <el-empty description="暂无记录" />
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'RecordTable',
  props: {
    records: {
      type: Array,
      default: () => []
    },
    loading: {
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
    currentPage: {
      type: Number,
      default: 1
    },
    showPagination: {
      type: Boolean,
      default: true
    }
  },
  emits: ['current-change'],
  setup(props, { emit }) {
    const handleCurrentChange = (page) => {
      emit('current-change', page)
    }
    
    return {
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 40px 0;
}

.mt-4 {
  margin-top: 16px;
}
</style>
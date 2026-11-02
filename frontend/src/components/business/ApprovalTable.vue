<template>
  <el-table :data="filteredRecords" style="width: 100%" v-loading="loading">
    <slot name="columns"></slot>
    <el-table-column label="操作" width="220" fixed="right">
      <template #default="scope">
        <div class="action-cell">
          <template v-if="isActionable(scope.row.status)">
            <el-button link class="action-btn action-btn--approve" @click="$emit('approve', scope.row)">批准</el-button>
            <span class="action-sep" aria-hidden="true"></span>
            <el-button link class="action-btn action-btn--reject" @click="$emit('reject', scope.row)">拒绝</el-button>
            <slot name="actionButtons" :row="scope.row"></slot>
          </template>
          <el-button v-else link class="action-btn action-btn--view" @click="$emit('view', scope.row)">查看</el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
const ACTIONABLE_STATUSES = ['pending', 'leader_approved', 'pending_leader', 'final_approval_created']

export default {
  name: 'ApprovalTable',
  props: {
    records: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['approve', 'reject', 'view'],
  computed: {
    filteredRecords() {
      return this.records
    }
  },
  methods: {
    isActionable(status) {
      return ACTIONABLE_STATUSES.includes(status)
    }
  }
}
</script>

<style scoped>
.action-cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.action-btn {
  height: 28px;
  padding: 0 9px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    color var(--eam-duration-fast) var(--eam-ease);
}

.action-btn--approve {
  color: #34634f;
}

.action-btn--approve:hover {
  color: #2a5342;
  background: var(--eam-success-soft);
}

.action-btn--reject {
  color: #8c4646;
}

.action-btn--reject:hover {
  color: #7a3a3a;
  background: var(--eam-danger-soft);
}

.action-btn--view {
  color: var(--eam-primary-deep);
}

.action-btn--view:hover {
  color: var(--eam-primary-deep);
  background: var(--eam-primary-soft);
}

.action-sep {
  width: 1px;
  height: 12px;
  background: var(--eam-line);
}
</style>

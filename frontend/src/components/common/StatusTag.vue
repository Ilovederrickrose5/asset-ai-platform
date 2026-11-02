<template>
  <span class="status-chip" :class="`status-chip--${statusType}`">
    <i class="status-dot" aria-hidden="true"></i>
    <span>{{ statusText }}</span>
  </span>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    status: {
      type: String,
      default: 'pending'
    },
    statusMap: {
      type: Object,
      default: () => ({
        pending: { text: '待审批', type: 'warning' },
        approved: { text: '已批准', type: 'success' },
        rejected: { text: '已拒绝', type: 'danger' },
        in_progress: { text: '维修中', type: 'info' },
        completed: { text: '已完成', type: 'success' },
        final_approval_created: { text: '已提交终审', type: 'info' },
        pending_leader: { text: '待领导审批', type: 'warning' },
        leader_approved: { text: '领导已批准', type: 'primary' }
      })
    }
  },
  computed: {
    statusText() {
      return this.statusMap[this.status]?.text || this.status
    },
    statusType() {
      return this.statusMap[this.status]?.type || 'info'
    }
  }
}
</script>

<style scoped>
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 9px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
  border: 1px solid transparent;
  border-radius: var(--eam-radius-pill);
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.85;
}

.status-chip--warning {
  color: #8a6430;
  background: var(--eam-warning-soft);
  border-color: rgba(169, 122, 60, 0.14);
}

.status-chip--success {
  color: #34634f;
  background: var(--eam-success-soft);
  border-color: rgba(63, 125, 100, 0.14);
}

.status-chip--danger {
  color: #8c4646;
  background: var(--eam-danger-soft);
  border-color: rgba(164, 87, 87, 0.14);
}

.status-chip--info {
  color: var(--eam-ink-500);
  background: #f1f3f6;
  border-color: rgba(20, 24, 33, 0.06);
}

.status-chip--primary {
  color: var(--eam-primary-deep);
  background: var(--eam-primary-soft);
  border-color: rgba(61, 86, 128, 0.14);
}
</style>

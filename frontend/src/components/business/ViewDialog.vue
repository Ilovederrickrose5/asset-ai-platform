<template>
  <el-dialog
    v-model="dialogVisible"
    width="640px"
    align-center
    class="eam-dialog view-dialog"
  >
    <template #header>
      <div class="dialog-head">
        <div class="dialog-head__text">
          <strong>{{ title }}</strong>
          <span>{{ subtitle || '申请流程与审批信息' }}</span>
        </div>
      </div>
    </template>

    <el-descriptions :column="2" border class="view-desc">
      <slot></slot>
    </el-descriptions>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ref, watch } from 'vue'

export default {
  name: 'ViewDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '详情'
    },
    subtitle: {
      type: String,
      default: ''
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.visible)

    watch(() => props.visible, (newVal) => {
      dialogVisible.value = newVal
    })

    watch(dialogVisible, (newVal) => {
      emit('update:visible', newVal)
    })

    const close = () => {
      dialogVisible.value = false
    }

    return {
      dialogVisible,
      close
    }
  }
}
</script>

<style scoped>
.dialog-head__text strong {
  display: block;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--eam-ink-900);
}

.dialog-head__text span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: var(--eam-ink-400);
}

.view-desc :deep(.el-descriptions__body) {
  background: transparent;
}

.view-desc :deep(.el-descriptions__table) {
  border-color: var(--eam-line-soft);
}

.view-desc :deep(.el-descriptions__cell) {
  padding: 11px 14px;
  font-size: 13px;
  line-height: 1.55;
  border-color: var(--eam-line-soft);
}

.view-desc :deep(.el-descriptions__label) {
  width: 108px;
  font-size: 12px;
  font-weight: 500;
  color: var(--eam-ink-400);
  background: var(--eam-surface-muted, #f7f8fa);
}

.view-desc :deep(.el-descriptions__content) {
  color: var(--eam-ink-800);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>

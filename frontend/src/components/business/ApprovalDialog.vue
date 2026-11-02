<template>
  <el-dialog
    v-model="dialogVisible"
    width="520px"
    align-center
    class="eam-dialog approval-dialog"
  >
    <template #header>
      <div class="dialog-head">
        <span class="dialog-head__icon" :class="approvalType === 'approve' ? 'is-approve' : 'is-reject'">
          <el-icon>
            <CircleCheck v-if="approvalType === 'approve'" />
            <CircleClose v-else />
          </el-icon>
        </span>
        <div class="dialog-head__text">
          <strong>{{ dialogTitle }}</strong>
          <span>{{ dialogHint }}</span>
        </div>
      </div>
    </template>

    <el-form :model="approvalForm" ref="approvalFormRef" label-position="top" class="approval-form">
      <div class="approval-detail">
        <slot name="details"></slot>
      </div>

      <el-form-item label="审批备注" prop="approvalRemark">
        <el-input
          v-model="approvalForm.approvalRemark"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          :placeholder="approvalType === 'approve' ? '补充说明（可选），将同步给申请人' : '请说明拒绝原因，便于申请人调整后重新提交'"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button
          :type="approvalType === 'approve' ? 'primary' : 'danger'"
          :plain="approvalType === 'reject'"
          :loading="loading"
          @click="submit"
        >
          {{ approvalType === 'approve' ? '确认批准' : '确认拒绝' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch, computed } from 'vue'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'

export default {
  name: 'ApprovalDialog',
  components: { CircleCheck, CircleClose },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    approvalType: {
      type: String,
      default: 'approve',
      validator: (value) => ['approve', 'reject'].includes(value)
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'submit', 'close'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.visible)
    const approvalFormRef = ref(null)
    const approvalForm = reactive({
      approvalRemark: ''
    })

    const dialogTitle = computed(() => (props.approvalType === 'approve' ? '批准申请' : '拒绝申请'))
    const dialogHint = computed(() =>
      props.approvalType === 'approve'
        ? '批准后流程将继续流转，审批结果同步至申请人'
        : '拒绝后申请结束，申请人可修改后重新提交'
    )

    watch(() => props.visible, (newVal) => {
      dialogVisible.value = newVal
      if (newVal) {
        approvalForm.approvalRemark = ''
      }
    })

    watch(dialogVisible, (newVal) => {
      emit('update:visible', newVal)
    })

    const close = () => {
      dialogVisible.value = false
      emit('close')
    }

    const submit = () => {
      emit('submit', approvalForm)
    }

    return {
      dialogVisible,
      approvalFormRef,
      approvalForm,
      dialogTitle,
      dialogHint,
      close,
      submit
    }
  }
}
</script>

<style scoped>
.dialog-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-head__icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  font-size: 18px;
  border-radius: 12px;
}

.dialog-head__icon.is-approve {
  color: #34634f;
  background: var(--eam-success-soft);
}

.dialog-head__icon.is-reject {
  color: #8c4646;
  background: var(--eam-danger-soft);
}

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

.approval-form {
  margin-top: 2px;
}

.approval-detail {
  margin-bottom: 18px;
  padding: 14px 16px 4px;
  background: var(--eam-surface-muted, #f7f8fa);
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-md);
}

.approval-detail :deep(.el-form-item) {
  margin-bottom: 10px;
}

.approval-detail :deep(.el-form-item__label) {
  padding-bottom: 2px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--eam-ink-400);
}

.approval-detail :deep(.el-form-item__content) {
  font-size: 13px;
  color: var(--eam-ink-800);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

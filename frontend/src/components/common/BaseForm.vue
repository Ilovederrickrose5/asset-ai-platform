<template>
  <el-form
    :model="model"
    :rules="rules"
    ref="formRef"
    :label-width="labelWidth"
    :validate-on-rule-change="false"
  >
    <slot></slot>

    <div class="form-actions" :style="{ paddingLeft: labelWidth }">
      <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      <el-button @click="reset">重置</el-button>
    </div>
  </el-form>
</template>

<script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'BaseForm',
  props: {
    model: {
      type: Object,
      required: true
    },
    rules: {
      type: Object,
      default: () => {}
    },
    labelWidth: {
      type: String,
      default: '100px'
    }
  },
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const submitting = ref(false)
    
    const submit = async () => {
      if (!formRef.value) return
      
      formRef.value.validate((valid) => {
        if (valid) {
          submitting.value = true
          Promise.resolve(emit('submit')).then(() => {
            submitting.value = false
          }).catch((error) => {
            ElMessage.error('提交失败')
            submitting.value = false
          })
        }
      })
    }
    
    const reset = () => {
      if (formRef.value) {
        formRef.value.resetFields()
        emit('reset')
      }
    }
    
    return {
      formRef,
      submitting,
      submit,
      reset
    }
  }
}
</script>

<style scoped>
.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  padding-top: 18px;
  border-top: 1px solid var(--eam-line-soft);
}
</style>
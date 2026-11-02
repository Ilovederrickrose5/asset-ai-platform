<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    :destroy-on-close="destroyOnClose"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-width="labelWidth"
      :label-position="labelPosition"
    >
      <slot :form="formData" :isEdit="isEdit">
        <!-- 动态表单字段 -->
        <template v-for="field in fields" :key="field.prop">
          <el-form-item
            :label="field.label"
            :prop="field.prop"
            :required="isEdit ? (field.requiredOnEdit || field.required) : (field.requiredOnAdd || field.required)"
          >
            <!-- 输入框 -->
            <el-input
              v-if="field.type === 'input'"
              v-model="formData[field.prop]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="field.disabled || (isEdit && field.disabledOnEdit)"
              :clearable="field.clearable !== false"
            />
            
            <!-- 密码框 -->
            <el-input
              v-else-if="field.type === 'password'"
              v-model="formData[field.prop]"
              type="password"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :disabled="field.disabled || (isEdit && field.disabledOnEdit)"
              show-password
            />
            
            <!-- 文本域 -->
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="formData[field.prop]"
              type="textarea"
              :rows="field.rows || 3"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
            
            <!-- 数字输入 -->
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="formData[field.prop]"
              :min="field.min"
              :max="field.max"
              :precision="field.precision"
              :placeholder="field.placeholder || `请输入${field.label}`"
              style="width: 100%"
            />
            
            <!-- 选择框 -->
            <el-select
              v-else-if="field.type === 'select'"
              v-model="formData[field.prop]"
              :placeholder="field.placeholder || `请选择${field.label}`"
              :clearable="field.clearable !== false"
              style="width: 100%"
              @change="(val) => field.onChange && field.onChange(val)"
            >
              <el-option
                v-for="option in field.options"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            
            <!-- 单选框组 -->
            <el-radio-group
              v-else-if="field.type === 'radio'"
              v-model="formData[field.prop]"
            >
              <el-radio
                v-for="option in field.options"
                :key="option.value"
                :label="option.value"
              >
                {{ option.label }}
              </el-radio>
            </el-radio-group>
            
            <!-- 日期选择器 -->
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="formData[field.prop]"
              type="date"
              :placeholder="field.placeholder || '选择日期'"
              style="width: 100%"
            />
            
            <!-- 静态文本（用于显示自动生成的编码等） -->
            <el-input
              v-else-if="field.type === 'static'"
              :value="typeof field.value === 'function' ? field.value() : field.value || formData[field.prop]"
              :placeholder="field.placeholder || ''"
              readonly
              :disabled="true"
            />
            
            <!-- 自定义内容 -->
            <slot
              v-else-if="field.type === 'custom'"
              :name="`field-${field.prop.toLowerCase()}`"
              :field="field"
              :form="formData"
              :isEdit="isEdit"
            />
          </el-form-item>
        </template>
      </slot>
    </el-form>
    
    <template #footer>
      <slot name="footer" :form="formData" :isEdit="isEdit" :submitting="submitting">
        <el-button @click="handleCancel" :disabled="submitting">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ submitting ? '提交中...' : '确定' }}
        </el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '500px'
  },
  fields: {
    type: Array,
    default: () => []
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  formRules: {
    type: Object,
    default: () => ({})
  },
  labelWidth: {
    type: String,
    default: '100px'
  },
  labelPosition: {
    type: String,
    default: 'right'
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  destroyOnClose: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit', 'cancel', 'closed'])

const formRef = ref(null)
const submitting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const localFormData = reactive({ ...props.formData })

watch(() => props.formData, (newVal) => {
  Object.assign(localFormData, newVal)
}, { deep: true, immediate: true })

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate((valid) => {
      if (valid) {
        submitting.value = true
        emit('submit', { ...localFormData, isEdit: props.isEdit })
      }
    })
  } catch (error) {
    // 忽略验证错误
  }
}

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}

const handleClose = () => {
  formRef.value?.resetFields()
  emit('closed')
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const setSubmitting = (value) => {
  submitting.value = value
}

// 暴露方法给父组件
defineExpose({
  resetForm,
  setSubmitting,
  formRef
})
</script>

<style scoped>
/* 对话框样式可以根据需要自定义 */
</style>

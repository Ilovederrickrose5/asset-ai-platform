<template>
  <BaseForm
    :model="form"
    :rules="rules"
    label-width="100px"
    @submit="handleSubmit"
    @reset="handleReset"
  >
    <el-form-item label="选择资产" prop="assetId">
      <AssetSelector
        :model-value="form.assetId"
        :assets="assetOptions"
        placeholder="请选择要报废的资产"
        @update:model-value="(value) => form.assetId = value"
      />
    </el-form-item>
    
    <el-form-item label="报废类型" prop="disposalType">
      <el-select
        v-model="form.disposalType"
        placeholder="请选择报废类型"
      >
        <el-option label="损坏" value="damaged" />
        <el-option label="过时" value="obsolete" />
        <el-option label="丢失" value="lost" />
        <el-option label="其他" value="other" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="估计价值" prop="estimatedValue">
      <el-input
        v-model.number="form.estimatedValue"
        placeholder="请输入估计价值"
      />
    </el-form-item>
    
    <el-form-item label="报废原因" prop="disposalReason">
      <el-input
        v-model="form.disposalReason"
        type="textarea"
        :rows="4"
        placeholder="请填写报废原因"
      />
    </el-form-item>
  </BaseForm>
</template>

<script>
import { reactive } from 'vue'
import BaseForm from '../common/BaseForm.vue'
import AssetSelector from '../common/AssetSelector.vue'

export default {
  name: 'DisposalForm',
  components: {
    BaseForm,
    AssetSelector
  },
  props: {
    assetOptions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const form = reactive({
      assetId: null,
      disposalType: '',
      estimatedValue: null,
      disposalReason: ''
    })
    
    const rules = {
      assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
      disposalType: [{ required: true, message: '请选择报废类型', trigger: 'change' }],
      disposalReason: [{ required: true, message: '请填写报废原因', trigger: 'blur' }]
    }
    
    const handleSubmit = () => {
      emit('submit', form)
    }
    
    const handleReset = () => {
      emit('reset')
    }
    
    return {
      form,
      rules,
      handleSubmit,
      handleReset
    }
  }
}
</script>
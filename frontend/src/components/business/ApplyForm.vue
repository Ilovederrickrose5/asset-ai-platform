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
        :placeholder="applicationType === 'MAINTENANCE' ? '请选择要维修的资产' : '请选择要领用的资产'"
        @update:model-value="(value) => form.assetId = value"
      />
    </el-form-item>

    <el-form-item v-if="applicationType === 'MAINTENANCE'" label="维修类型" prop="maintenanceType">
      <el-select v-model="form.maintenanceType" placeholder="请选择维修类型">
        <el-option label="日常维护" value="daily" />
        <el-option label="故障维修" value="repair" />
        <el-option label="预防性维护" value="preventive" />
      </el-select>
    </el-form-item>
    
    <el-form-item :label="applicationType === 'MAINTENANCE' ? '维修原因' : '领用原因'" prop="reason">
      <el-input
        v-model="form.reason"
        type="textarea"
        :rows="4"
        :placeholder="applicationType === 'MAINTENANCE' ? '请填写维修原因' : '请填写领用原因'"
      />
    </el-form-item>
  </BaseForm>
</template>

<script>
import { reactive, computed } from 'vue'
import BaseForm from '../common/BaseForm.vue'
import AssetSelector from '../common/AssetSelector.vue'

export default {
  name: 'ApplyForm',
  components: {
    BaseForm,
    AssetSelector
  },
  props: {
    assetOptions: {
      type: Array,
      default: () => []
    },
    applicationType: {
      type: String,
      default: 'RECEIVE'
    }
  },
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const form = reactive({
      assetId: null,
      reason: '',
      maintenanceType: ''
    })
    
    const rules = {
      assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
      reason: [{ required: true, message: '请填写申请原因', trigger: 'blur' }],
      maintenanceType: [{ required: true, message: '请选择维修类型', trigger: 'change' }]
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
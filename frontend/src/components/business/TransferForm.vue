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
        placeholder="请选择要转移的资产"
        @update:model-value="(value) => form.assetId = value"
      />
    </el-form-item>
    
    <el-form-item label="接收人" prop="transfereeId">
      <el-select
        v-model="form.transfereeId"
        placeholder="请选择接收人"
        @change="handleTransfereeChange"
      >
        <el-option
          v-for="user in userOptions"
          :key="user.id"
          :label="user.realName || user.username"
          :value="user.id"
        />
      </el-select>
    </el-form-item>
    
    <el-form-item label="接收部门" prop="transfereeDeptId">
      <el-input
        v-model="form.transfereeDeptName"
        disabled
        placeholder="接收人所在部门"
      />
    </el-form-item>
    
    <el-form-item label="转移原因" prop="transferReason">
      <el-input
        v-model="form.transferReason"
        type="textarea"
        :rows="4"
        placeholder="请填写转移原因"
      />
    </el-form-item>
  </BaseForm>
</template>

<script>
import { reactive } from 'vue'
import BaseForm from '../common/BaseForm.vue'
import AssetSelector from '../common/AssetSelector.vue'

export default {
  name: 'TransferForm',
  components: {
    BaseForm,
    AssetSelector
  },
  props: {
    assetOptions: {
      type: Array,
      default: () => []
    },
    userOptions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const form = reactive({
      assetId: null,
      transfereeId: null,
      transfereeName: '',
      transfereeDeptId: null,
      transfereeDeptName: '',
      transferReason: ''
    })
    
    const rules = {
      assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
      transfereeId: [{ required: true, message: '请选择接收人', trigger: 'change' }],
      transferReason: [{ required: true, message: '请填写转移原因', trigger: 'blur' }]
    }
    
    const handleTransfereeChange = (userId) => {
      const user = props.userOptions.find(u => u.id === userId)
      if (user) {
        form.transfereeName = user.realName || user.username
        // 兼容 departmentId 和 deptId 两种字段名
        form.transfereeDeptId = user.departmentId || user.deptId
        // 兼容 departmentName 和 deptName 两种字段名
        form.transfereeDeptName = user.departmentName || user.deptName || ''
      }
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
      handleTransfereeChange,
      handleSubmit,
      handleReset
    }
  }
}
</script>
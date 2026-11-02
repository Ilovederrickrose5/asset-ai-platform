<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :filterable="filterable"
    :remote-method="handleSearch"
    :loading="loading"
    style="width: 100%"
    @update:model-value="handleChange"
  >
    <el-option
      v-for="asset in assets"
      :key="asset.id"
      :label="assetLabel(asset)"
      :value="asset.id"
    />
  </el-select>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'AssetSelector',
  props: {
    modelValue: {
      type: [Number, null],
      default: null
    },
    assets: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: '请选择资产'
    },
    filterable: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const loading = ref(false)
    
    const assetLabel = (asset) => {
      return `${asset.assetName} - ${asset.model || ''} (${asset.assetNo || ''})`
    }
    
    const handleSearch = (query) => {
      // 这里可以实现远程搜索逻辑
      // 目前使用本地过滤
      loading.value = true
      setTimeout(() => {
        loading.value = false
      }, 300)
    }
    
    const handleChange = (value) => {
      emit('update:modelValue', value)
      emit('change', value)
    }
    
    return {
      loading,
      assetLabel,
      handleSearch,
      handleChange
    }
  }
}
</script>
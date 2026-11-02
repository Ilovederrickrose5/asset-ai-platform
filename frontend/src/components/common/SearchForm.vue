<template>
  <el-form :model="searchForm" inline class="search-form">
    <el-form-item
      v-for="field in fields"
      :key="field.prop"
      :label="field.label"
    >
      <!-- 输入框 -->
      <el-input
        v-if="field.type === 'input'"
        v-model="searchForm[field.prop]"
        :placeholder="field.placeholder || `请输入${field.label}`"
        clearable
        :style="{ width: field.width || '200px' }"
      />
      
      <!-- 选择框 -->
      <el-select
        v-else-if="field.type === 'select'"
        v-model="searchForm[field.prop]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        clearable
        :style="{ width: field.width || '150px' }"
      >
        <el-option
          v-for="option in field.options"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <!-- 日期选择器 -->
      <el-date-picker
        v-else-if="field.type === 'date'"
        v-model="searchForm[field.prop]"
        type="date"
        :placeholder="field.placeholder || '选择日期'"
        :style="{ width: field.width || '150px' }"
      />
      
      <!-- 日期范围 -->
      <el-date-picker
        v-else-if="field.type === 'date-range'"
        v-model="searchForm[field.prop]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :style="{ width: field.width || '240px' }"
      />
      
      <!-- 自定义内容 -->
      <slot
        v-else-if="field.type === 'custom'"
        :name="`field-${field.prop}`"
        :field="field"
        :form="searchForm"
      />
    </el-form-item>
    
    <el-form-item>
      <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
      <el-button @click="handleReset" :icon="RefreshRight">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'

const props = defineProps({
  fields: {
    type: Array,
    required: true,
    default: () => []
  },
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'reset'])

const searchForm = reactive({ ...props.modelValue })

watch(() => props.modelValue, (newVal) => {
  Object.assign(searchForm, newVal)
}, { deep: true })

watch(searchForm, (newVal) => {
  emit('update:modelValue', { ...newVal })
}, { deep: true })

const handleSearch = () => {
  emit('search', { ...searchForm })
}

const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = undefined
  })
  emit('reset')
  emit('search', {})
}
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .search-form :deep(.el-form-item) {
    margin-bottom: 10px;
  }
  
  .search-form :deep(.el-input),
  .search-form :deep(.el-select),
  .search-form :deep(.el-date-editor) {
    width: 100%;
    max-width: 250px;
  }
}

@media (max-width: 480px) {
  .search-form :deep(.el-input),
  .search-form :deep(.el-select),
  .search-form :deep(.el-date-editor) {
    max-width: 100%;
  }
}
</style>

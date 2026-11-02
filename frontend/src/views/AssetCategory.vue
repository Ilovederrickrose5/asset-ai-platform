<template>
  <ManagementPage
    title="资产分类管理"
    page-class="category"
    :data="categoryList"
    :loading="loading"
    :columns="tableColumns"
    :form-fields="formFields"
    :form-rules="formRules"
    :has-permission="true"
    @add="handleAdd"
    @edit="handleEdit"
    @delete="handleDelete"
    @submit="handleSubmit"
    ref="managementPageRef"
  >
    <!-- 状态列自定义 -->
    <template #column-status="{ value }">
      <el-tag :type="value === 1 ? 'success' : 'danger'">
        {{ value === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
    
    <!-- 创建时间列自定义 -->
    <template #column-createTime="{ value }">
      {{ formatTime(value) }}
    </template>
  </ManagementPage>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'
import ManagementPage from '../components/business/ManagementPage.vue'
import { hasAdminPermission } from '../utils/permission.js'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const categoryList = ref([])
const managementPageRef = ref(null)

const getToken = () => userStore.token

// 表格列配置
const tableColumns = [
  { prop: 'categoryName', label: '分类名称', minWidth: '150' },
  { prop: 'categoryCode', label: '分类编码', minWidth: '120' },
  { prop: 'description', label: '分类描述', minWidth: '200', showOverflowTooltip: true },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'custom', prop: 'createTime', label: '创建时间', width: '180' },
  { type: 'actions', label: '操作', width: '200', fixed: 'right' }
]

// 父分类选项
const parentOptions = ref([
  { label: '无', value: 0 }
])

// 表单字段配置
const formFields = computed(() => [
  { 
    type: 'input', 
    prop: 'categoryName', 
    label: '分类名称', 
    required: true,
    placeholder: '请输入分类名称'
  },
  { 
    type: 'select', 
    prop: 'parentId', 
    label: '父分类',
    placeholder: '请选择父分类',
    options: parentOptions.value,
    defaultValue: 0,
    onChange: handleParentChange
  },
  { 
    type: 'static', 
    prop: 'categoryCode', 
    label: '分类编码',
    value: '',
    readonly: true
  },
  { 
    type: 'textarea', 
    prop: 'description', 
    label: '分类描述',
    rows: 3,
    placeholder: '请输入分类描述'
  },
  { 
    type: 'radio', 
    prop: 'status', 
    label: '状态',
    defaultValue: 1,
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
])

// 表单验证规则
const formRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

// 获取分类列表
const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await axios.get('/asset-categories')
    if (response.data.code === 200) {
      categoryList.value = response.data.data
      // 更新父分类选项
      updateParentOptions(response.data.data)
    }
  } catch (error) {
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

// 更新父分类选项
const updateParentOptions = (categories) => {
  parentOptions.value = [
    { label: '无', value: 0 }
  ]
  categories.forEach(category => {
    if (category.parentId === 0) {
      parentOptions.value.push({
        label: category.categoryName,
        value: category.id
      })
    }
  })
}

// 处理父分类变化
const handleParentChange = async (parentId) => {
  try {
    const response = await axios.get(`/asset-categories/generate-code?parentId=${parentId}`)
    if (response.data.code === 200) {
      // 更新表单中的分类编码
      const formData = managementPageRef.value?.getFormData() || {}
      formData.categoryCode = response.data.data
      managementPageRef.value?.setFormData(formData)
    }
  } catch (error) {
    ElMessage.error('生成分类编码失败')
  }
}

// 处理新增
const handleAdd = async () => {
  // 重置表单并生成默认编码
  try {
    const response = await axios.get('/asset-categories/generate-code?parentId=0')
    if (response.data.code === 200) {
      managementPageRef.value?.setFormData({
        categoryName: '',
        parentId: 0,
        categoryCode: response.data.data,
        description: '',
        status: 1
      })
    }
  } catch (error) {
    ElMessage.error('生成分类编码失败')
  }
}

// 处理编辑
const handleEdit = (row) => {
  managementPageRef.value?.setFormData({
    id: row.id,
    categoryName: row.categoryName,
    parentId: row.parentId || 0,
    categoryCode: row.categoryCode,
    description: row.description || '',
    status: row.status
  })
}

// 处理删除
const handleDelete = async (row) => {
  try {
    const response = await axios.delete(`/asset-categories/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      fetchCategories()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 处理提交
const handleSubmit = async ({ isEdit, id, ...formData }) => {
  managementPageRef.value?.setSubmitting(true)
  try {
    let response
    if (isEdit) {
      response = await axios.put(`/asset-categories/${id}`, formData)
    } else {
      response = await axios.post('/asset-categories', formData)
    }
    if (response.data.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '创建成功')
      managementPageRef.value?.closeDialog()
      fetchCategories()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error(isEdit ? '更新失败' : '创建失败')
  } finally {
    managementPageRef.value?.setSubmitting(false)
  }
}

// 时间格式化
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-container {
  padding: 20px;
}
</style>

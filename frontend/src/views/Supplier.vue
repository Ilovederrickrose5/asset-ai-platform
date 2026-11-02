<template>
  <ManagementPage
    title="供应商管理"
    page-class="supplier"
    :data="supplierList"
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
import { ref, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'
import ManagementPage from '../components/business/ManagementPage.vue'

const loading = ref(false)
const supplierList = ref([])
const managementPageRef = ref(null)

// 表格列配置
const tableColumns = [
  { prop: 'supplierName', label: '供应商名称', minWidth: '150' },
  { prop: 'contactPerson', label: '联系人', minWidth: '120' },
  { prop: 'phone', label: '联系电话', minWidth: '120' },
  { prop: 'address', label: '地址', minWidth: '200', showOverflowTooltip: true },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'custom', prop: 'createTime', label: '创建时间', width: '180' },
  { type: 'actions', label: '操作', width: '180', fixed: 'right' }
]

// 表单字段配置
const formFields = [
  { 
    type: 'input', 
    prop: 'supplierName', 
    label: '供应商名称', 
    required: true,
    placeholder: '请输入供应商名称'
  },
  { 
    type: 'input', 
    prop: 'contactPerson', 
    label: '联系人',
    required: true,
    placeholder: '请输入联系人'
  },
  { 
    type: 'input', 
    prop: 'phone', 
    label: '联系电话',
    required: true,
    placeholder: '请输入手机号码'
  },
  { 
    type: 'textarea', 
    prop: 'address', 
    label: '地址',
    required: true,
    rows: 2,
    placeholder: '请输入详细地址'
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
]

// 表单验证规则
const formRules = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

// 获取供应商列表
const fetchSuppliers = async () => {
  loading.value = true
  try {
    const response = await axios.get('/suppliers')
    if (response.data.code === 200) {
      supplierList.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('获取供应商列表失败')
  } finally {
    loading.value = false
  }
}

// 新增/编辑对话框由 ManagementPage 内部管理
const handleAdd = () => {}

const handleEdit = () => {}

// 处理删除
const handleDelete = async (row) => {
  try {
    const response = await axios.delete(`/suppliers/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      fetchSuppliers()
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
      response = await axios.put(`/suppliers/${id}`, formData)
    } else {
      response = await axios.post('/suppliers', formData)
    }
    if (response.data.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '创建成功')
      managementPageRef.value?.closeDialog()
      fetchSuppliers()
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
  fetchSuppliers()
})
</script>

<style scoped>
.supplier-container {
  padding: 20px;
}
</style>
<template>
  <ManagementPage
    title="部门管理"
    page-class="department"
    :data="departmentList"
    :loading="loading"
    :columns="tableColumns"
    :form-fields="formFields"
    :form-rules="formRules"
    :has-permission="hasAdminPermission()"
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
    
    <!-- 负责人列自定义 -->
    <template #column-leader="{ row }">
      {{ getLeaderName(row.leader) }}
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
const departmentList = ref([])
const managementPageRef = ref(null)

const getToken = () => userStore.token

// 用户列表（用于选择部门负责人）
const userList = ref([])

// 表格列配置
const tableColumns = [
  { prop: 'deptName', label: '部门名称', minWidth: '150' },
  { prop: 'deptCode', label: '部门编码', minWidth: '120' },
  { type: 'custom', prop: 'leader', label: '部门负责人', minWidth: '120' },
  { prop: 'description', label: '部门描述', minWidth: '200', showOverflowTooltip: true },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'custom', prop: 'createTime', label: '创建时间', width: '180' },
  { type: 'actions', label: '操作', width: '180', fixed: 'right' }
]

// 自动生成的部门编码
const autoDeptCode = ref('')

// 表单字段配置
const formFields = computed(() => [
  { 
    type: 'input', 
    prop: 'deptName', 
    label: '部门名称', 
    required: true,
    placeholder: '请输入部门名称'
  },
  { 
    type: 'static', 
    prop: 'deptCode', 
    label: '部门编码',
    value: () => autoDeptCode.value,
    readonly: true
  },
  { 
    type: 'select', 
    prop: 'leader', 
    label: '部门负责人',
    placeholder: '请选择部门负责人',
    clearable: true,
    options: userList.value.map(user => ({
      label: user.realName || user.username,
      value: user.id
    }))
  },
  { 
    type: 'textarea', 
    prop: 'description', 
    label: '部门描述',
    rows: 3,
    placeholder: '请输入部门描述'
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
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

// 获取部门列表
const fetchDepartments = async () => {
  loading.value = true
  try {
    const response = await axios.get('/departments')
    if (response.data.code === 200) {
      departmentList.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('获取部门列表失败')
  } finally {
    loading.value = false
  }
}

// 处理新增
const handleAdd = async () => {
  try {
    const response = await axios.get('/departments/generate-code')
    if (response.data.code === 200) {
      autoDeptCode.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('生成部门编码失败')
  }
}

// 编辑对话框由 ManagementPage 内部管理
const handleEdit = () => {}

// 处理删除
const handleDelete = async (row) => {
  try {
    const response = await axios.delete(`/departments/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      fetchDepartments()
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
    // 对于新增部门，确保添加自动生成的编码
    if (!isEdit) {
      formData.deptCode = autoDeptCode.value
    }
    
    let response
    if (isEdit) {
      response = await axios.put(`/departments/${id}`, formData)
    } else {
      response = await axios.post('/departments', formData)
    }
    if (response.data.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '创建成功')
      managementPageRef.value?.closeDialog()
      fetchDepartments()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error(isEdit ? '更新失败' : '创建失败')
  } finally {
    managementPageRef.value?.setSubmitting(false)
  }
}

// 获取用户列表
const fetchUsers = async () => {
  try {
    const response = await axios.get('/users')
    if (response.data.code === 200) {
      userList.value = response.data.data
    }
  } catch (error) {
    console.error('获取用户列表失败', error)
  }
}

// 获取负责人名称
const getLeaderName = (leaderId) => {
  if (!leaderId) return '-'
  const user = userList.value.find(u => u.id === leaderId)
  return user ? (user.realName || user.username) : '-'
}

// 时间格式化
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchDepartments()
  fetchUsers()
})
</script>

<style scoped>
.department-container {
  padding: 20px;
}
</style>

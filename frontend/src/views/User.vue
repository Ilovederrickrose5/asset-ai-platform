<template>
  <div class="user-container">
    <ManagementPage
      title="员工信息管理"
      page-class="user"
      :data="userList"
      :loading="loading"
      :columns="tableColumns"
      :form-fields="formFields"
      :form-rules="formRules"
      :dialog-width="'600px'"
      :has-permission="hasAdminPermission()"
      :show-delete-button="false"
      @add="handleAdd"
      @edit="handleEdit"
      @submit="handleSubmit"
      ref="managementPageRef"
    >
      <!-- 部门列自定义 -->
      <template #column-departmentName="{ value }">
        {{ value || '-' }}
      </template>
      
      <!-- 职位列自定义 -->
      <template #column-position="{ value }">
        {{ value || '-' }}
      </template>
      
      <!-- 状态列自定义 -->
      <template #column-status="{ value }">
        <el-tag :type="value === 1 ? 'success' : 'danger'">
          {{ value === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
      
      <!-- 操作列自定义 -->
      <template #actions="{ row }">
        <el-button type="primary" size="small" @click="managementPageRef.handleEdit(row)">编辑</el-button>
        <el-button type="warning" size="small" @click="handleRole(row)">角色</el-button>
        <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
      </template>
      
      <!-- 表单字段自定义 - 部门选择 -->
      <template #form-departmentid="{ form }">
        <el-select v-model="form.departmentId" placeholder="请选择部门" clearable style="width: 100%">
          <el-option
            v-for="dept in departmentOptions"
            :key="dept.id"
            :label="dept.deptName"
            :value="dept.id"
          />
        </el-select>
      </template>
      
      <!-- 表单字段自定义 - 角色选择 -->
      <template #form-role="{ form }">
        <el-select v-model="form.role" placeholder="请选择角色" clearable style="width: 100%">
          <el-option
            v-for="role in roleOptions"
            :key="role.id"
            :label="role.name"
            :value="role.code"
          />
        </el-select>
      </template>
    </ManagementPage>

    <!-- 角色分配对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRole" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import ManagementPage from '../components/business/ManagementPage.vue'
import { hasAdminPermission } from '../utils/permission.js'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const userList = ref([])
const departmentOptions = ref([])
const roleOptions = ref([])
const managementPageRef = ref(null)

// 角色对话框相关
const roleDialogVisible = ref(false)
const currentUser = ref({})
const currentUserId = ref(null)
const selectedRole = ref(null)

const getToken = () => userStore.token

// 表格列配置
const tableColumns = [
  { prop: 'username', label: '用户名', minWidth: '120' },
  { prop: 'realName', label: '真实姓名', minWidth: '100' },
  { prop: 'employeeNo', label: '员工编号', minWidth: '120' },
  { type: 'custom', prop: 'departmentName', label: '所属部门', minWidth: '120' },
  { type: 'custom', prop: 'position', label: '职位', minWidth: '120' },
  { prop: 'email', label: '邮箱', minWidth: '180', showOverflowTooltip: true },
  { prop: 'phone', label: '手机号', minWidth: '120' },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'actions', label: '操作', width: '280' }
]

// 表单字段配置
const formFields = [
  { 
    type: 'input', 
    prop: 'username', 
    label: '用户名', 
    required: true,
    placeholder: '请输入用户名',
    disabledOnEdit: true
  },
  { 
    type: 'password', 
    prop: 'password', 
    label: '密码',
    placeholder: '请输入密码（编辑时可不填）',
    requiredOnAdd: true
  },
  { 
    type: 'input', 
    prop: 'realName', 
    label: '真实姓名',
    placeholder: '请输入真实姓名'
  },
  { 
    type: 'input', 
    prop: 'employeeNo', 
    label: '员工编号',
    placeholder: '请输入员工编号'
  },
  { 
    type: 'custom', 
    prop: 'departmentId', 
    label: '所属部门',
    required: true
  },
  { 
    type: 'input', 
    prop: 'position', 
    label: '职位',
    placeholder: '请输入职位'
  },
  { 
    type: 'input', 
    prop: 'email', 
    label: '邮箱',
    placeholder: '请输入邮箱'
  },
  { 
    type: 'input', 
    prop: 'phone', 
    label: '手机号',
    placeholder: '请输入手机号'
  },
  { 
    type: 'custom', 
    prop: 'role', 
    label: '角色',
    required: true
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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await axios.get('/users')
    if (response.data.code === 200) {
      userList.value = response.data.data.map(user => {
        // 兼容 deptId 和 departmentId 两种字段名
        const userDeptId = user.deptId || user.departmentId
        const department = departmentOptions.value.find(dept => dept.id === userDeptId)
        return {
          ...user,
          departmentName: department ? department.deptName : '-'
        }
      })
    }
  } catch (error) {
    ElMessage.error('获取员工列表失败')
  } finally {
    loading.value = false
  }
}

// 获取部门列表
const fetchDepartments = async () => {
  try {
    const response = await axios.get('/departments/active')
    if (response.data.code === 200) {
      departmentOptions.value = response.data.data
    }
  } catch (error) {
    console.error('获取部门列表失败', error)
  }
}

// 获取角色列表
const fetchRoles = async () => {
  try {
    const response = await axios.get('/roles')
    if (response.data.code === 200) {
      roleOptions.value = response.data.data
    }
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

// 新增/编辑对话框由 ManagementPage 内部管理
const handleAdd = () => {}

const handleEdit = () => {}

// 处理删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除员工 "${row.realName || row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await axios.delete(`/users/${row.id}`)
      if (response.data.code === 200) {
        ElMessage.success('删除成功')
        fetchUsers()
      } else {
        ElMessage.error(response.data.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 处理提交
const handleSubmit = async ({ isEdit, id, password, departmentId, role, ...formData }) => {
  managementPageRef.value?.setSubmitting(true)
  try {
    let response
    // 将 departmentId 转换为 deptId，匹配后端实体类字段
    const submitData = {
      ...formData,
      deptId: departmentId,
      role: role || 'user'  // 如果角色为空，默认设置为普通用户
    }
    if (isEdit) {
      if (password) {
        submitData.password = password
      }
      response = await axios.put(`/users/${id}`, submitData)
    } else {
      response = await axios.post('/users', { password, ...submitData })
    }
    if (response.data.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '创建成功')
      managementPageRef.value?.closeDialog()
      fetchUsers()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || (isEdit ? '更新失败' : '创建失败')
    ElMessage.error(errorMsg)
  } finally {
    managementPageRef.value?.setSubmitting(false)
  }
}

// 处理角色分配
const handleRole = (row) => {
  currentUser.value = row
  currentUserId.value = row.id
  // 设置用户当前角色，如果用户已有角色
  if (row.role) {
    const role = roleOptions.value.find(r => r.code === row.role)
    selectedRole.value = role ? role.id : null
  } else {
    selectedRole.value = null
  }
  roleDialogVisible.value = true
}

// 提交角色分配
const handleRoleSubmit = async () => {
  try {
    const response = await axios.put(`/users/${currentUserId.value}/roles`, [selectedRole.value])
    if (response.data.code === 200) {
      ElMessage.success('角色分配成功')
      roleDialogVisible.value = false
      fetchUsers()
    } else {
      ElMessage.error(response.data.message || '角色分配失败')
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || '角色分配失败'
    ElMessage.error(errorMsg)
  }
}

onMounted(async () => {
  await fetchDepartments()
  fetchUsers()
  fetchRoles()
})
</script>

<style scoped>
.user-container {
  padding: 20px;
}
</style>

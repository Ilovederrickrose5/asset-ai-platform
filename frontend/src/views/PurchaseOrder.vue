<template>
  <ManagementPage
    title="采购订单管理"
    :data="orders"
    :columns="tableColumns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-rules="formRules"
    :loading="loading"
    :show-back-button="true"
    :show-add-button="true"
    :show-edit-button="true"
    :show-delete-button="true"
    :show-pagination="true"
    :total="total"
    :page-size="pageSize"
    dialog-width="800px"
    @search="handleSearch"
    @submit="handleSubmit"
    @delete="handleDelete"
    ref="managementPageRef"
  >
    <!-- 自定义状态列 -->
    <template #column-status="{ value }">
      <el-tag :type="getStatusType(value)">
        {{ getStatusText(value) }}
      </el-tag>
    </template>

    <!-- 自定义单价列 -->
    <template #column-unitPrice="{ value }">
      ¥{{ formatPrice(value) }}
    </template>

    <!-- 自定义总金额列 -->
    <template #column-totalAmount="{ value }">
      ¥{{ formatPrice(value) }}
    </template>

    <!-- 自定义日期列 -->
    <template #column-orderDate="{ value }">
      {{ formatTime(value) }}
    </template>

    <!-- 自定义操作列 -->
    <template #actions="{ row }">
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="viewOrder(row)" :icon="View">
          查看
        </el-button>
        <el-button type="primary" size="small" @click="handleEdit(row)" :icon="Edit" :disabled="row.status === 'completed' || row.status === 'COMPLETED'">
          编辑
        </el-button>
        <template v-if="row.status === 'completed' || row.status === 'COMPLETED'">
          <el-tag type="success" size="small">已到货</el-tag>
        </template>
        <el-button 
          v-else 
          type="success" 
          size="small" 
          @click="handleCompleteOrder(row)" 
          :icon="Check"
        >
          到货
        </el-button>
        <template v-if="row.status === 'pending' || row.status === 'PENDING'">
          <el-button type="danger" size="small" @click="handleDelete(row)" :icon="Delete">
            删除
          </el-button>
        </template>
      </div>
    </template>

    <!-- 自定义表单底部 -->
    <template #form-footer="{ form, isEdit, submitting }">
      <el-button @click="cancelForm" :disabled="submitting">取消</el-button>
      <el-button type="primary" @click="handleSubmit({ ...form, isEdit })" :loading="submitting">
        {{ submitting ? '提交中...' : '确定' }}
      </el-button>
    </template>
  </ManagementPage>

  <!-- 查看订单对话框 -->
  <el-dialog
    title="查看采购订单"
    v-model="viewDialogVisible"
    width="600px"
  >
    <el-descriptions :column="2" border>
      <el-descriptions-item label="订单编号">{{ viewData.id }}</el-descriptions-item>
      <el-descriptions-item label="订单号">{{ viewData.orderNumber }}</el-descriptions-item>
      <el-descriptions-item label="物品名称">{{ viewData.itemName }}</el-descriptions-item>
      <el-descriptions-item label="规格型号">{{ viewData.specification }}</el-descriptions-item>
      <el-descriptions-item label="数量">{{ viewData.quantity }}</el-descriptions-item>
      <el-descriptions-item label="单位">{{ viewData.unit }}</el-descriptions-item>
      <el-descriptions-item label="单价">¥{{ formatPrice(viewData.unitPrice) }}</el-descriptions-item>
      <el-descriptions-item label="总金额">¥{{ formatPrice(viewData.totalAmount) }}</el-descriptions-item>
      <el-descriptions-item label="供应商">{{ viewData.supplierName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下单日期">{{ formatTime(viewData.orderDate) }}</el-descriptions-item>
      <el-descriptions-item label="预计交付日期">{{ formatTime(viewData.expectedDeliveryDate) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="实际交付日期">{{ formatTime(viewData.actualDeliveryDate) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="getStatusType(viewData.status)">{{ getStatusText(viewData.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="付款状态">{{ getPaymentStatusText(viewData.paymentStatus) }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ viewData.creatorName }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatTime(viewData.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="部门">{{ viewData.departmentName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ viewData.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Delete, Check } from '@element-plus/icons-vue'
import axios from '../utils/request'
import ManagementPage from '../components/business/ManagementPage.vue'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const managementPageRef = ref(null)

// 订单列表数据
const orders = ref([])
const total = ref(0)
const pageSize = ref(10)
const loading = ref(false)
const loadingData = ref(false)

// 采购需求申请列表
const purchaseRequests = ref([])
// 供应商列表
const suppliers = ref([])
// 资产分类列表
const assetCategories = ref([])

// 查看订单对话框数据
const viewDialogVisible = ref(false)
const viewData = ref({})

// 搜索字段配置
const searchFields = ref([
  {
    prop: 'status',
    label: '订单状态',
    type: 'select',
    options: [
      { label: '全部', value: '' },
      { label: '待处理', value: 'pending' },
      { label: '已完成', value: 'completed' },
      { label: '已取消', value: 'cancelled' }
    ]
  }
])

// 表格列配置
const tableColumns = ref([
  { prop: 'id', label: '订单编号', width: 100 },
  { prop: 'orderNumber', label: '订单号', width: 180 },
  { prop: 'itemName', label: '物品名称', minWidth: 150 },
  { prop: 'specification', label: '规格型号', width: 120 },
  { prop: 'quantity', label: '数量', width: 80 },
  { prop: 'unit', label: '单位', width: 80 },
  { prop: 'unitPrice', label: '单价', width: 100, type: 'custom' },
  { prop: 'totalAmount', label: '总金额', width: 120, type: 'custom' },
  { prop: 'supplierName', label: '供应商', width: 180 },
  { prop: 'orderDate', label: '下单日期', width: 150, type: 'custom' },
  { prop: 'status', label: '状态', width: 100, type: 'custom' },
  { prop: 'actions', label: '操作', width: 240, fixed: 'right', type: 'actions' }
])

// 表单字段配置
const formFields = computed(() => [
  { 
    prop: 'purchaseRequestId', 
    label: '采购需求申请', 
    type: 'select', 
    required: true,
    placeholder: '选择采购需求申请',
    options: purchaseRequests.value.map(r => ({ label: `${r.id} - ${r.itemName}`, value: r.id })),
    disabledOnEdit: true,
    onChange: handlePurchaseRequestChange
  },
  { prop: 'orderNumber', label: '订单号', type: 'input', required: true, placeholder: '输入订单号' },
  { prop: 'itemName', label: '物品名称', type: 'input', required: true, placeholder: '输入物品名称' },
  { prop: 'specification', label: '规格型号', type: 'input', placeholder: '输入规格型号' },
  { prop: 'quantity', label: '数量', type: 'number', required: true, defaultValue: 1 },
  { prop: 'unit', label: '单位', type: 'input', placeholder: '输入单位' },
  { prop: 'unitPrice', label: '单价', type: 'number', required: true, defaultValue: 0 },
  { prop: 'totalAmount', label: '总金额', type: 'number', required: true, defaultValue: 0, readonly: true },
  { 
    prop: 'categoryId', 
    label: '资产分类', 
    type: 'select', 
    required: true,
    placeholder: '选择资产分类',
    options: assetCategories.value.map(c => ({ label: c.categoryName, value: c.id })),
    onChange: handleCategoryChange
  },
  { 
    prop: 'supplierId', 
    label: '供应商', 
    type: 'select', 
    required: true,
    placeholder: '选择供应商',
    options: suppliers.value.map(s => ({ label: s.supplierName, value: s.id })),
    onChange: handleSupplierChange
  },
  { prop: 'orderDate', label: '订单日期', type: 'date', required: true },
  { prop: 'expectedDeliveryDate', label: '预计交付日期', type: 'date' },
  { prop: 'remark', label: '备注', type: 'textarea', placeholder: '输入备注' }
])

// 表单验证规则
const formRules = ref({
  orderNumber: [
    { required: true, message: '请输入订单号', trigger: 'blur' },
    { min: 1, max: 50, message: '订单号长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  itemName: [
    { required: true, message: '请输入物品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '物品名称长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量必须大于 0', trigger: 'blur' }
  ],
  unitPrice: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '单价必须大于 0', trigger: 'blur' }
  ],
  totalAmount: [
    { required: true, message: '请输入总金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '总金额必须大于 0', trigger: 'blur' }
  ],
  supplierId: [
    { required: true, message: '请选择供应商', trigger: 'blur' }
  ],
  orderDate: [
    { required: true, message: '请选择订单日期', trigger: 'blur' }
  ]
})

// 获取订单列表
const fetchOrders = async (page = 1, searchForm = {}) => {
  loading.value = true
  try {
    let url = `/purchase-orders?page=${page}&size=${pageSize.value}`
    if (searchForm.status) {
      url += `&status=${searchForm.status}`
    }
    const response = await axios.get(url)
    if (response.data.code === 200) {
      orders.value = response.data.data.records
      total.value = response.data.data.total
    }
  } catch (error) {
    console.error('获取采购订单失败', error)
    ElMessage.error('获取采购订单失败')
  } finally {
    loading.value = false
  }
}

// 获取采购需求申请列表
const fetchPurchaseRequests = async () => {
  loadingData.value = true
  try {
    const response = await axios.get('/purchase-requests/status/approved')
    if (response.data.code === 200) {
      purchaseRequests.value = response.data.data
    }
  } catch (error) {
    console.error('获取采购需求申请失败', error)
  } finally {
    loadingData.value = false
  }
}

// 获取供应商列表
const fetchSuppliers = async () => {
  loadingData.value = true
  try {
    const response = await axios.get('/suppliers')
    if (response.data.code === 200) {
      suppliers.value = response.data.data
    }
  } catch (error) {
    console.error('获取供应商列表失败', error)
  } finally {
    loadingData.value = false
  }
}

// 获取资产分类列表
const fetchAssetCategories = async () => {
  loadingData.value = true
  try {
    const response = await axios.get('/asset-categories')
    if (response.data.code === 200) {
      assetCategories.value = response.data.data
    }
  } catch (error) {
    console.error('获取资产分类列表失败', error)
    console.error('API调用路径: /asset-categories')
    console.error('错误详情:', error.response?.data || error.message)
  } finally {
    loadingData.value = false
  }
}

// 自动生成订单号
const generateOrderNumber = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  const formData = managementPageRef.value?.getFormData()
  if (formData) {
    formData.orderNumber = `PO${year}${month}${day}${hours}${minutes}${seconds}${random}`
  }
}

// 采购需求申请选择变化处理
const handlePurchaseRequestChange = (value) => {
  if (value) {
    const formData = managementPageRef.value?.getFormData()
    const request = purchaseRequests.value.find(r => r.id === parseInt(value))
    if (request && formData) {
      formData.itemName = request.itemName
      formData.specification = request.specification
      formData.quantity = request.quantity
      formData.unit = request.unit
      formData.unitPrice = request.estimatedUnitPrice || 0
      calculateTotalAmount(formData)
    }
  }
}

// 资产分类选择变化处理
const handleCategoryChange = (value) => {
  const formData = managementPageRef.value?.getFormData()
  if (!formData) return

  const category = assetCategories.value.find(c => c.id === value)
  if (category) {
    formData.categoryName = category.categoryName
  }
}

// 供应商选择变化处理
const handleSupplierChange = async (value) => {
  const formData = managementPageRef.value?.getFormData()
  if (!formData) return

  if (typeof value === 'string') {
    try {
      const response = await axios.post('/suppliers', { supplierName: value })
      if (response.data.code === 200) {
        const newSupplier = response.data.data
        suppliers.value.push(newSupplier)
        formData.supplierId = newSupplier.id
        formData.supplierName = newSupplier.supplierName
        ElMessage.success('供应商创建成功')
      }
    } catch (error) {
      console.error('创建供应商失败', error)
      ElMessage.error('创建供应商失败')
    }
  } else {
    const supplier = suppliers.value.find(s => s.id === value)
    if (supplier) {
      formData.supplierName = supplier.supplierName
    }
  }
}

// 计算总金额
const calculateTotalAmount = (formData) => {
  if (formData.quantity && formData.unitPrice) {
    formData.totalAmount = formData.quantity * formData.unitPrice
  } else {
    formData.totalAmount = 0
  }
}

// 监听数量和单价变化，自动计算总金额
watch(
  () => {
    const formData = managementPageRef.value?.getFormData()
    return formData ? [formData.quantity, formData.unitPrice] : [0, 0]
  },
  ([quantity, unitPrice]) => {
    const formData = managementPageRef.value?.getFormData()
    if (formData) {
      calculateTotalAmount(formData)
    }
  },
  { deep: true }
)

// 搜索处理
const handleSearch = (searchForm) => {
  fetchOrders(1, searchForm)
}

// 编辑处理
const handleEdit = (row) => {
  managementPageRef.value?.handleEdit(row)
}

// 取消表单
const cancelForm = () => {
  managementPageRef.value?.closeDialog()
}

// 提交处理
const handleSubmit = async ({ isEdit, id, ...form }) => {
  try {
    // 计算总金额
    form.totalAmount = form.quantity * form.unitPrice

    // 获取供应商名称
    const supplier = suppliers.value.find(s => s.id === form.supplierId)
    if (supplier) {
      form.supplierName = supplier.supplierName
    }

    // 获取当前用户信息
    const user = userStore.user
    if (user) {
      form.creatorId = user.id
      form.creatorName = user.realName
      form.departmentId = user.departmentId
    }

    let response
    if (isEdit && id) {
      response = await axios.put(`/purchase-orders/${id}`, form)
    } else {
      // 新创建的订单默认状态为"待处理"
      response = await axios.post('/purchase-orders', { ...form, status: 'pending' })
    }

    if (response.data.code === 200) {
      ElMessage.success('保存成功')
      managementPageRef.value?.closeDialog()
      fetchOrders()
    }
  } catch (error) {
    console.error('保存订单失败', error)
    ElMessage.error('保存订单失败')
  }
}

// 删除处理
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await axios.delete(`/purchase-orders/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      fetchOrders()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除订单失败', error)
      ElMessage.error('删除订单失败')
    }
  }
}

// 到货处理（完成订单并自动创建资产）
const handleCompleteOrder = async (row) => {
  try {
    await ElMessageBox.confirm('确认订单已到货？系统将自动创建对应数量的资产。', '提示', {
      confirmButtonText: '确认到货',
      cancelButtonText: '取消',
      type: 'info'
    })
    const response = await axios.put(`/purchase-orders/${row.id}/complete`)
    if (response.data.code === 200) {
      ElMessage.success(`订单已到货，已自动创建 ${row.quantity} 个资产`)
      fetchOrders()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('订单到货处理失败', error)
      ElMessage.error(error.response?.data?.message || '订单到货处理失败')
    }
  }
}

// 查看订单
const viewOrder = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    pending: 'warning',
    PENDING: 'warning',
    completed: 'success',
    COMPLETED: 'success',
    cancelled: 'danger',
    CANCELLED: 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    pending: '待处理',
    PENDING: '待处理',
    completed: '已完成',
    COMPLETED: '已完成',
    cancelled: '已取消',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

// 获取付款状态文本
const getPaymentStatusText = (status) => {
  const statusMap = {
    unpaid: '未付款',
    UNPAID: '未付款',
    paid: '已付款',
    PAID: '已付款',
    partial: '部分付款',
    PARTIAL: '部分付款'
  }
  return statusMap[status] || status
}

// 格式化价格
const formatPrice = (price) => {
  if (!price) return '0.00'
  return Number(price).toFixed(2)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 初始化数据
onMounted(() => {
  fetchOrders()
  fetchPurchaseRequests()
  fetchSuppliers()
  fetchAssetCategories()

  // 检查URL参数中是否有requestId，如果有则自动打开新建订单对话框
  const requestId = route.query.requestId
  if (requestId) {
    setTimeout(() => {
      managementPageRef.value?.handleAdd()
      const formData = managementPageRef.value?.getFormData()
      if (formData) {
        formData.purchaseRequestId = parseInt(requestId)
        handlePurchaseRequestChange(parseInt(requestId))
      }
    }, 500)
  }
})
</script>

<style scoped>
.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
}
</style>
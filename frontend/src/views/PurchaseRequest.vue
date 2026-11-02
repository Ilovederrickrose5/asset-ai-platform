<template>
  <ManagementPage
    title="采购需求申请"
    :data="requests"
    :columns="tableColumns"
    :search-fields="searchFields"
    :form-fields="formFields"
    :form-rules="formRules"
    :loading="loading"
    :show-back-button="true"
    :show-add-button="true"
    :show-edit-button="false"
    :show-delete-button="false"
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

    <!-- 自定义预估单价列 -->
    <template #column-estimatedUnitPrice="{ value }">
      ¥{{ formatPrice(value) }}
    </template>

    <!-- 自定义总金额列 -->
    <template #column-totalAmount="{ value }">
      ¥{{ formatPrice(value) }}
    </template>

    <!-- 自定义申请时间列 -->
    <template #column-applicationDate="{ value }">
      {{ formatTime(value) }}
    </template>

    <!-- 自定义操作列 -->
    <template #actions="{ row }">
      <el-button type="primary" size="small" @click="handleView(row)" :icon="View">
        查看
      </el-button>
      <el-button 
        v-if="row.status === 'pending'" 
        type="primary" 
        size="small" 
        @click="handleEdit(row)" 
        :icon="Edit"
      >
        编辑
      </el-button>
      <el-button 
        v-if="row.status === 'pending'" 
        type="danger" 
        size="small" 
        @click="handleDelete(row)" 
        :icon="Delete"
      >
        删除
      </el-button>
      <el-button 
        v-if="row.status === 'approved'" 
        type="success" 
        size="small" 
        @click="handleCreateOrder(row)" 
        :icon="ShoppingCart"
      >
        创建订单
      </el-button>
    </template>

    <!-- 自定义表单底部 -->
    <template #form-footer="{ form, isEdit, submitting }">
      <el-button @click="cancelForm" :disabled="submitting">取消</el-button>
      <el-button type="primary" @click="handleSubmit({ ...form, isEdit })" :loading="submitting">
        {{ submitting ? '提交中...' : '确定' }}
      </el-button>
    </template>
  </ManagementPage>

  <!-- 查看申请对话框 -->
  <el-dialog v-model="viewDialogVisible" title="查看采购需求申请" width="600px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="申请编号">{{ viewData.id }}</el-descriptions-item>
      <el-descriptions-item label="物品名称">{{ viewData.itemName }}</el-descriptions-item>
      <el-descriptions-item label="规格型号">{{ viewData.specification }}</el-descriptions-item>
      <el-descriptions-item label="数量">{{ viewData.quantity }}</el-descriptions-item>
      <el-descriptions-item label="单位">{{ viewData.unit }}</el-descriptions-item>
      <el-descriptions-item label="预估单价">¥{{ formatPrice(viewData.estimatedUnitPrice) }}</el-descriptions-item>
      <el-descriptions-item label="总金额">¥{{ formatPrice(viewData.totalAmount) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="getStatusType(viewData.status)">{{ getStatusText(viewData.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="申请人">{{ viewData.applicantName }}</el-descriptions-item>
      <el-descriptions-item label="申请时间">{{ formatTime(viewData.applicationDate) }}</el-descriptions-item>
      <el-descriptions-item label="审批人">{{ viewData.approverName }}</el-descriptions-item>
      <el-descriptions-item label="审批时间">{{ formatTime(viewData.approvalDate) }}</el-descriptions-item>
      <el-descriptions-item label="采购原因" :span="2">{{ viewData.purchaseReason }}</el-descriptions-item>
      <el-descriptions-item label="审批意见" :span="2">{{ viewData.approvalRemark }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <!-- 新建订单对话框 -->
  <el-dialog
    title="新建订单"
    v-model="createOrderDialogVisible"
    width="800px"
  >
    <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="采购需求申请" required>
            <el-select v-model="orderForm.purchaseRequestId" placeholder="选择采购需求申请" disabled>
              <el-option
                :label="`${orderForm.purchaseRequestId} - ${orderForm.itemName}`"
                :value="orderForm.purchaseRequestId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="订单号" required>
            <el-input v-model="orderForm.orderNumber" placeholder="输入订单号">
              <template #append>
                <el-button @click="generateOrderNumber" size="small">自动生成</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="物品名称" required>
            <el-input v-model="orderForm.itemName" placeholder="输入物品名称" />
          </el-form-item>
          <el-form-item label="规格型号">
            <el-input v-model="orderForm.specification" placeholder="输入规格型号" />
          </el-form-item>
          <el-form-item label="数量" required>
            <el-input 
              v-model.number="orderForm.quantity" 
              type="number" 
              placeholder="输入数量" 
              @input="calculateOrderTotalAmount"
            />
          </el-form-item>
          <el-form-item label="单位">
            <el-input v-model="orderForm.unit" placeholder="输入单位" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单价" required>
            <el-input 
              v-model.number="orderForm.unitPrice" 
              type="number" 
              step="0.01" 
              placeholder="输入单价" 
              @input="calculateOrderTotalAmount"
            />
          </el-form-item>
          <el-form-item label="总金额" required>
            <el-input 
              v-model.number="orderForm.totalAmount" 
              type="number" 
              step="0.01" 
              placeholder="输入总金额" 
              readonly
            />
          </el-form-item>
          <el-form-item label="供应商" required>
            <el-select 
              v-model="orderForm.supplierId" 
              placeholder="选择供应商"
              filterable
              allow-create
              default-first-option
              :loading="loadingData"
              @change="handleOrderSupplierChange"
            >
              <el-option
                v-for="supplier in suppliers"
                :key="supplier.id"
                :label="supplier.supplierName"
                :value="supplier.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="订单日期" required>
            <el-date-picker
              v-model="orderForm.orderDate"
              type="date"
              placeholder="选择订单日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="预计交付日期">
            <el-date-picker
              v-model="orderForm.expectedDeliveryDate"
              type="date"
              placeholder="选择预计交付日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="状态" required>
            <el-select v-model="orderForm.status" placeholder="选择状态">
              <el-option label="待处理" value="PENDING"></el-option>
              <el-option label="已完成" value="COMPLETED"></el-option>
              <el-option label="已取消" value="CANCELLED"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注">
        <el-input v-model="orderForm.remark" type="textarea" placeholder="输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="createOrderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOrder">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Delete, ShoppingCart } from '@element-plus/icons-vue'
import axios from '../utils/request'
import ManagementPage from '../components/business/ManagementPage.vue'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const managementPageRef = ref(null)

// 数据
const loading = ref(false)
const loadingData = ref(false)
const requests = ref([])
const currentUser = ref(null)

// 对话框
const viewDialogVisible = ref(false)
const viewData = ref({})

// 订单相关数据
const createOrderDialogVisible = ref(false)
const orderForm = ref({
  purchaseRequestId: '',
  orderNumber: '',
  itemName: '',
  specification: '',
  quantity: 1,
  unit: '',
  unitPrice: 0,
  totalAmount: 0,
  supplierId: '',
  supplierName: '',
  orderDate: new Date(),
  expectedDeliveryDate: '',
  status: 'PENDING',
  remark: ''
})
const orderRules = ref({
  orderNumber: [
    { required: true, message: '请输入订单号', trigger: 'blur' },
    { min: 1, max: 50, message: '订单号长度应在1-50个字符之间', trigger: 'blur' }
  ],
  itemName: [
    { required: true, message: '请输入物品名称', trigger: 'blur' },
    { min: 1, max: 100, message: '物品名称长度应在1-100个字符之间', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '数量必须大于0', trigger: 'blur' }
  ],
  unitPrice: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '单价必须大于0', trigger: 'blur' }
  ],
  totalAmount: [
    { required: true, message: '请输入总金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '总金额必须大于0', trigger: 'blur' }
  ],
  supplierId: [
    { required: true, message: '请选择供应商', trigger: 'change' }
  ],
  orderDate: [
    { required: true, message: '请选择订单日期', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
})
const orderFormRef = ref(null)
const suppliers = ref([])

// 搜索字段配置
const searchFields = ref([
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '全部', value: '' },
      { label: '待审批', value: 'pending' },
      { label: '已批准', value: 'approved' },
      { label: '已拒绝', value: 'rejected' }
    ]
  }
])

// 表格列配置
const tableColumns = ref([
  { prop: 'id', label: '申请编号', width: 100 },
  { prop: 'itemName', label: '物品名称', width: 150 },
  { prop: 'specification', label: '规格型号', width: 150 },
  { prop: 'quantity', label: '数量', width: 80 },
  { prop: 'unit', label: '单位', width: 80 },
  { prop: 'estimatedUnitPrice', label: '预估单价', width: 120, type: 'custom' },
  { prop: 'totalAmount', label: '总金额', width: 120, type: 'custom' },
  { prop: 'status', label: '状态', width: 100, type: 'custom' },
  { prop: 'applicationDate', label: '申请时间', width: 180, type: 'custom' },
  { prop: 'approverName', label: '审批人', width: 100 },
  { prop: 'approvalRemark', label: '审批意见', minWidth: 150, showOverflowTooltip: true },
  { prop: 'actions', label: '操作', width: 220, fixed: 'right', type: 'actions' }
])

// 表单字段配置
const formFields = ref([
  { prop: 'itemName', label: '物品名称', type: 'input', required: true },
  { prop: 'specification', label: '规格型号', type: 'input' },
  { prop: 'quantity', label: '数量', type: 'number', required: true, defaultValue: 1 },
  { prop: 'unit', label: '单位', type: 'input', required: true },
  { prop: 'estimatedUnitPrice', label: '预估单价', type: 'number', required: true, defaultValue: 0 },
  { prop: 'totalAmount', label: '总金额', type: 'static', value: () => {
    const formData = managementPageRef.value?.getFormData() || {}
    const quantity = Number(formData.quantity) || 0
    const price = Number(formData.estimatedUnitPrice) || 0
    return '¥' + (quantity * price).toFixed(2)
  }},
  { prop: 'purchaseReason', label: '采购原因', type: 'textarea', required: true }
])

// 表单验证规则
const formRules = ref({
  itemName: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  estimatedUnitPrice: [{ required: true, message: '请输入预估单价', trigger: 'blur' }],
  purchaseReason: [{ required: true, message: '请输入采购原因', trigger: 'blur' }]
})

// 计算总金额
const totalAmount = computed(() => {
  const formData = managementPageRef.value?.getFormData()
  if (formData && formData.estimatedUnitPrice && formData.quantity) {
    return '¥' + (formData.estimatedUnitPrice * formData.quantity).toFixed(2)
  }
  return '¥0.00'
})

// 获取当前用户
const getCurrentUser = () => {
  if (userStore.user) {
    currentUser.value = userStore.user
  }
}

// 获取申请列表
const fetchRequests = async (searchForm = {}) => {
  loading.value = true
  try {
    if (!currentUser.value) {
      ElMessage.error('用户未登录，请重新登录')
      router.push('/login')
      return
    }
    let url = `/purchase-requests/applicant/${currentUser.value.id}`
    if (searchForm.status) {
      url = `/purchase-requests/applicant/${currentUser.value.id}/status/${searchForm.status}`
    }
    const response = await axios.get(url)
    if (response.data.code === 200) {
      const data = response.data.data
      // 后端返回的是Page分页对象，实际数据在content属性中
      requests.value = data.content || data
    }
  } catch (error) {
    console.error('获取采购需求申请失败', error)
    ElMessage.error('获取采购需求申请失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = (searchForm) => {
  fetchRequests(searchForm)
}

// 编辑处理
const handleEdit = (row) => {
  managementPageRef.value?.handleEdit(row)
}

// 查看处理
const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

// 删除处理
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此采购需求申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await axios.delete(`/purchase-requests/${row.id}`)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      fetchRequests()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

// 取消表单
const cancelForm = () => {
  managementPageRef.value?.closeDialog()
}

// 提交处理
const handleSubmit = async ({ isEdit, id, ...form }) => {
  try {
    // 自动计算总金额
    const quantity = Number(form.quantity) || 0
    const estimatedUnitPrice = Number(form.estimatedUnitPrice) || 0
    const totalAmount = (quantity * estimatedUnitPrice).toFixed(2)
    
    let response
    if (isEdit && id) {
      response = await axios.put(`/purchase-requests/${id}`, { ...form, totalAmount })
    } else {
      response = await axios.post('/purchase-requests', { ...form, totalAmount })
    }

    if (response.data.code === 200) {
      ElMessage.success(isEdit ? '更新成功' : '提交成功')
      managementPageRef.value?.closeDialog()
      fetchRequests()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 加载供应商列表
const fetchSuppliers = async () => {
  loadingData.value = true
  try {
    const response = await axios.get('/suppliers')
    if (response.data.code === 200) {
      suppliers.value = response.data.data
    }
  } catch (error) {
    console.error('获取供应商列表失败:', error)
    ElMessage.error('获取供应商列表失败')
  } finally {
    loadingData.value = false
  }
}

// 生成订单号
const generateOrderNumber = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  orderForm.value.orderNumber = `PO${year}${month}${day}${hours}${minutes}${seconds}${random}`
}

// 计算订单总金额
const calculateOrderTotalAmount = () => {
  if (orderForm.value.quantity && orderForm.value.unitPrice) {
    orderForm.value.totalAmount = orderForm.value.quantity * orderForm.value.unitPrice
  }
}

// 处理订单供应商选择变化
const handleOrderSupplierChange = async (value) => {
  if (typeof value === 'string') {
    if (value.trim()) {
      try {
        const response = await axios.post('/suppliers', { supplierName: value })
        if (response.data.code === 200) {
          const newSupplier = response.data.data
          suppliers.value.push(newSupplier)
          orderForm.value.supplierId = newSupplier.id
          orderForm.value.supplierName = newSupplier.supplierName
          ElMessage.success('供应商创建成功')
        }
      } catch (error) {
        console.error('创建供应商失败', error)
        ElMessage.error('创建供应商失败')
      }
    }
  } else if (typeof value === 'number') {
    const supplier = suppliers.value.find(s => s.id === value)
    if (supplier) {
      orderForm.value.supplierId = supplier.id
      orderForm.value.supplierName = supplier.supplierName
    }
  }
}

// 保存订单
const saveOrder = async () => {
  if (!orderFormRef.value) return
  try {
    if (!orderForm.value.supplierId || !orderForm.value.supplierName) {
      ElMessage.error('请先选择或输入供应商')
      return
    }

    await orderFormRef.value.validate()
    const response = await axios.post('/purchase-orders', orderForm.value)

    if (response.data.code === 200) {
      ElMessage.success('订单创建成功')
      createOrderDialogVisible.value = false
      fetchRequests()
    } else {
      ElMessage.error('订单创建失败')
    }
  } catch (error) {
    console.error('保存订单失败:', error)
    ElMessage.error('订单创建失败')
  }
}

// 处理创建订单
const handleCreateOrder = (row) => {
  orderForm.value = {
    purchaseRequestId: row.id,
    orderNumber: '',
    itemName: row.itemName,
    specification: row.specification,
    quantity: row.quantity,
    unit: row.unit,
    unitPrice: row.estimatedUnitPrice || 0,
    totalAmount: row.totalAmount || 0,
    supplierId: '',
    supplierName: '',
    orderDate: new Date(),
    expectedDeliveryDate: '',
    status: 'PENDING',
    remark: ''
  }
  fetchSuppliers()
  createOrderDialogVisible.value = true
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已拒绝'
  }
  return textMap[status] || status
}

// 格式化价格
const formatPrice = (price) => {
  if (price == null) return '0.00'
  return parseFloat(price).toFixed(2)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  getCurrentUser()
  if (currentUser.value && currentUser.value.id) {
    fetchRequests()
  }
})
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
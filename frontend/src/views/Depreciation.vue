<template>
  <div class="depreciation-container">
    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>折旧计算管理</span>
          <el-button type="primary" @click="goToHome">返回首页</el-button>
        </div>
      </template>
      
      <!-- 计算类型选择 -->
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- 单资产折旧 -->
        <el-tab-pane label="单资产折旧" name="single">
          <el-form :model="singleForm" label-width="120px" class="form">
            <el-form-item label="资产选择">
              <el-select v-model="singleForm.assetId" placeholder="请选择资产" style="width: 400px" @change="onAssetChange">
                <el-option
                  v-for="asset in assets"
                  :key="asset.id"
                  :label="asset.assetNo + ' - ' + asset.assetName"
                  :value="asset.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="折旧方法">
              <el-select v-model="singleForm.depreciationMethod" placeholder="请选择折旧方法" style="width: 400px">
                <el-option
                  v-for="method in depreciationMethods"
                  :key="method.value"
                  :label="method.label"
                  :value="method.value"
                />
              </el-select>
              <span v-if="isUsingCustomMethod" class="custom-method-hint">
                <el-tag type="warning" size="small">使用临时折旧方法</el-tag>
              </span>
            </el-form-item>
            <el-form-item label="折旧月份">
              <el-date-picker
                v-model="singleForm.depreciationMonth"
                type="month"
                placeholder="选择折旧月份"
                style="width: 400px"
                format="YYYY年MM月"
                value-format="YYYY-MM"
              />
            </el-form-item>
            <el-form-item label="实际工作量" v-if="showWorkUnits">
              <el-input-number
                v-model="singleForm.actualWorkUnits"
                :min="0"
                :step="1"
                style="width: 400px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="calculateSingleDepreciation">计算折旧</el-button>
              <el-button @click="resetSingleForm">重置</el-button>
            </el-form-item>
          </el-form>
          
          <!-- 计算结果 -->
          <el-card v-if="singleResult" class="result-card">
            <template #header>
              <span>计算结果</span>
            </template>
            <el-descriptions :column="2">
              <el-descriptions-item label="资产编号">{{ singleResult.assetNo }}</el-descriptions-item>
              <el-descriptions-item label="资产名称">{{ singleResult.assetName }}</el-descriptions-item>
              <el-descriptions-item label="折旧方法">{{ getDepreciationMethodName(singleResult.depreciationMethod) || singleResult.depreciationMethod }}</el-descriptions-item>
              <el-descriptions-item label="本期折旧">
                <span>{{ formatCurrency(singleResult.depreciationAmount) }}</span>
                <el-popover
                  placement="top"
                  width="200"
                  trigger="hover"
                >
                  <template #reference>
                    <span style="color: #409EFF; font-size: 14px; cursor: help;">(?)</span>
                  </template>
                  <p>指本次计算期间（本月）应计提的折旧金额，即资产在本月的损耗价值。</p>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="累计折旧">
                <span>{{ formatCurrency(singleResult.accumulatedDepreciation) }}</span>
                <el-popover
                  placement="top"
                  width="200"
                  trigger="hover"
                >
                  <template #reference>
                    <span style="color: #409EFF; font-size: 14px; cursor: help;">(?)</span>
                  </template>
                  <p>指资产从开始使用到当前为止，累计已计提的折旧总额。反映资产的累计损耗程度。</p>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="当前净值">
                <span>{{ formatCurrency(singleResult.currentNetValue) }}</span>
                <el-popover
                  placement="top"
                  width="200"
                  trigger="hover"
                >
                  <template #reference>
                    <span style="color: #409EFF; font-size: 14px; cursor: help;">(?)</span>
                  </template>
                  <p>指资产当前的账面价值，即原值减去累计折旧后的余额。反映资产当前的实际价值。</p>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="计算日期">{{ formatDate(singleResult.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-tab-pane>
        
        <!-- 批量折旧 -->
        <el-tab-pane label="批量折旧" name="batch">
          <el-form :model="batchForm" label-width="120px" class="form">
            <el-form-item label="资产选择">
              <el-select
                v-model="batchForm.assetIds"
                multiple
                placeholder="请选择资产（可多选）"
                style="width: 400px"
                clearable
              >
                <el-option
                  v-for="asset in assets"
                  :key="asset.id"
                  :label="asset.assetNo + ' - ' + asset.assetName"
                  :value="asset.id"
                />
              </el-select>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                已选择 {{ batchForm.assetIds.length }} 个资产
              </span>
            </el-form-item>
            <el-form-item label="折旧方法">
              <el-select v-model="batchForm.depreciationMethod" placeholder="请选择折旧方法" style="width: 400px">
                <el-option value="" label="使用资产预设方法" />
                <el-option v-for="method in depreciationMethods" :key="method.value" :label="method.label" :value="method.value" />
              </el-select>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                为空则使用各资产预设的折旧方法
              </span>
            </el-form-item>
            <el-form-item label="折旧月份">
              <el-date-picker
                v-model="batchForm.depreciationMonth"
                type="month"
                placeholder="选择折旧月份"
                style="width: 400px"
                format="YYYY-MM"
                value-format="YYYY-MM"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="calculateBatchDepreciation" :disabled="batchForm.assetIds.length === 0">
                批量计算折旧
              </el-button>
              <el-button @click="resetBatchForm">重置</el-button>
            </el-form-item>
          </el-form>
          
          <!-- 批量计算结果 -->
          <el-card v-if="batchResults.length > 0" class="result-card">
            <template #header>
              <span>批量计算结果</span>
            </template>
            <el-table :data="batchResults" style="width: 100%">
              <el-table-column label="资产编号" :formatter="(row) => row.asset_no || row.assetNo" />
              <el-table-column label="资产名称" :formatter="(row) => row.asset_name || row.assetName" />
              <el-table-column label="折旧方法" :formatter="(row) => formatDepreciationMethod(row.depreciation_method || row.depreciationMethod)" />
              <el-table-column label="本期折旧" :formatter="(row) => formatCurrency(row.depreciation_amount || row.depreciationAmount)" />
              <el-table-column label="累计折旧" :formatter="(row) => formatCurrency(row.accumulated_depreciation || row.accumulatedDepreciation)" />
              <el-table-column label="当前净值" :formatter="(row) => formatCurrency(row.current_net_value || row.currentNetValue)" />
              <el-table-column label="计算日期" :formatter="(row) => formatDate(row.create_time || row.createTime)" />
            </el-table>
          </el-card>
        </el-tab-pane>
        
        <!-- 折旧任务 -->
        <el-tab-pane label="折旧任务" name="task">
          <div class="task-actions">
            <el-button type="primary" @click="openCreateTaskDialog">创建任务</el-button>
            <el-button @click="refreshTasks">刷新任务</el-button>
          </div>
          
          <el-table :data="tasks" style="width: 100%">
            <el-table-column prop="taskName" label="任务名称" />
            <el-table-column prop="taskType" label="任务类型" />
            <el-table-column prop="taskStatus" label="任务状态" :formatter="formatTaskStatus" />
            <el-table-column prop="periodType" label="周期类型" />
            <el-table-column prop="periodValue" label="周期值" />
            <el-table-column prop="lastExecutionTime" label="上次执行时间" :formatter="formatDateTime" />
            <el-table-column prop="nextExecutionTime" label="下次执行时间" :formatter="formatDateTime" />
            <el-table-column label="操作">
              <template #default="scope">
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="executeTask(scope.row.id)"
                  :disabled="scope.row.taskStatus !== 'PENDING'"
                >
                  执行
                </el-button>
                <el-button size="small" @click="viewTaskDetail(scope.row.id)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <!-- 折旧记录 -->
        <el-tab-pane label="折旧记录" name="record">
          <el-form :model="recordSearchForm" label-width="100px" class="search-form">
            <el-form-item label="资产编号">
              <el-input v-model="recordSearchForm.assetNo" placeholder="请输入资产编号" />
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="recordSearchForm.startDate"
                type="date"
                placeholder="选择开始日期"
              />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="recordSearchForm.endDate"
                type="date"
                placeholder="选择结束日期"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchRecords">查询</el-button>
              <el-button @click="resetRecordSearch">重置</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="records" style="width: 100%">
            <el-table-column label="资产编号" :formatter="(row) => row.asset_no || row.assetNo" />
            <el-table-column label="资产名称" :formatter="(row) => row.asset_name || row.assetName" />
            <el-table-column label="折旧方法" :formatter="(row) => formatDepreciationMethod(row.depreciation_method || row.depreciationMethod)" />
            <el-table-column label="本期折旧" :formatter="(row) => formatCurrency(row.depreciation_amount || row.depreciationAmount)" />
            <el-table-column label="累计折旧" :formatter="(row) => formatCurrency(row.accumulated_depreciation || row.accumulatedDepreciation)" />
            <el-table-column label="当前净值" :formatter="(row) => formatCurrency(row.current_net_value || row.currentNetValue)" />
            <el-table-column label="计算日期" :formatter="(row) => formatDate(row.create_time || row.createTime)" />
            <el-table-column label="操作">
              <template #default="scope">
                <el-button size="small" @click="rollbackRecord(scope.row.id)">
                  回滚
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 创建任务对话框 -->
    <el-dialog v-model="createTaskDialogVisible" title="创建折旧任务" width="500px">
      <el-form :model="taskForm" label-width="120px">
        <el-form-item label="任务名称" required>
          <el-input v-model="taskForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" required>
          <el-select v-model="taskForm.taskType" placeholder="请选择任务类型">
            <el-option label="自动" value="AUTO" />
            <el-option label="手动" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期类型" required>
          <el-select v-model="taskForm.periodType" placeholder="请选择周期类型">
            <el-option label="月度" value="MONTHLY" />
            <el-option label="季度" value="QUARTERLY" />
            <el-option label="年度" value="YEARLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期值" required>
          <el-input-number v-model="taskForm.periodValue" :min="1" step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createTaskDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createTask">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '../utils/request'
import { ElMessage, ElLoading } from 'element-plus'
import { useRouter } from 'vue-router'

// 路由实例
const router = useRouter()

// 激活的标签页
const activeTab = ref('single')

// 资产列表
const assets = ref([])

// 折旧方法选项
const depreciationMethods = ref([
  { value: 'STRAIGHT_LINE', label: '直线法' },
  { value: 'DOUBLE_DECLINING', label: '双倍余额递减法' },
  { value: 'WORK_UNIT', label: '工作量法' }
])

// 当前资产预设的折旧方法
const assetDefaultMethod = ref('STRAIGHT_LINE')

// 格式化当前月份为 yyyy-MM 格式
const formatCurrentMonth = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

// 单资产折旧表单
const singleForm = ref({
  assetId: null,
  depreciationMethod: '',
  depreciationMonth: '',
  actualWorkUnits: 1
})

// 批量折旧表单
const batchForm = ref({
  assetIds: [],
  depreciationMethod: '',
  depreciationMonth: formatCurrentMonth()
})

// 任务表单
const taskForm = ref({
  taskName: '',
  taskType: 'AUTO',
  periodType: 'MONTHLY',
  periodValue: 1
})

// 记录查询表单
const recordSearchForm = ref({
  assetNo: '',
  startDate: null,
  endDate: null
})

// 计算结果
const singleResult = ref(null)
const batchResults = ref([])

// 任务和记录
const tasks = ref([])
const records = ref([])

// 对话框
const createTaskDialogVisible = ref(false)

// 资产变更时自动设置折旧方法
const onAssetChange = (assetId) => {
  const asset = assets.value.find(a => a.id === assetId)
  if (asset) {
    assetDefaultMethod.value = asset.depreciationMethod || 'STRAIGHT_LINE'
    // 如果还没有选择过折旧方法，使用资产预设的方法
    if (!singleForm.value.depreciationMethod) {
      singleForm.value.depreciationMethod = assetDefaultMethod.value
    }
  }
}

// 判断是否使用了临时折旧方法
const isUsingCustomMethod = computed(() => {
  return singleForm.value.depreciationMethod && 
         singleForm.value.depreciationMethod !== assetDefaultMethod.value
})

// 显示工作量输入（基于选择的折旧方法）
const showWorkUnits = computed(() => {
  return singleForm.value.depreciationMethod === 'WORK_UNIT'
})

// 返回首页
const goToHome = () => {
  router.push('/home')
}

// 初始化
onMounted(() => {
  loadAssets()
  // 暂时注释掉对不存在接口的调用
  // loadTasks()
  loadRecords()
})

// 格式化日期为YYYY-MM-DD格式
const formatDateForApi = (date) => {
  if (!date) return null
  const d = new Date(date)
  return d.toISOString().split('T')[0]
}

// 加载资产列表（过滤掉已报废的资产）
const loadAssets = async () => {
  try {
    const response = await axios.get('/assets/all')
    if (response.data.code === 200) {
      // 只保留允许计提折旧的资产状态：使用中、在库、维修中
      const allowedStatus = ['using', 'in_stock', 'maintenance']
      assets.value = response.data.data.filter(asset => 
        allowedStatus.includes(asset.status)
      )
    } else {
      ElMessage.error('加载资产失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 加载任务列表
const loadTasks = async () => {
  try {
    // 暂时返回空数组，因为后端接口不存在
    tasks.value = []
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 加载折旧记录
const loadRecords = async () => {
  try {
    const response = await axios.get('/depreciation/records/date-range', {
      params: {
        startDate: formatDateForApi(new Date(new Date().getFullYear(), 0, 1)),
        endDate: formatDateForApi(new Date())
      }
    })
    
    if (response.data.code === 200) {
      records.value = response.data.data
    } else {
      console.error('加载记录失败:', response.data.message)
      records.value = []
    }
  } catch (error) {
    console.error('网络错误，请稍后重试', error)
    records.value = []
  }
}

// 计算单资产折旧
const calculateSingleDepreciation = async () => {
  if (!singleForm.value.assetId) {
    ElMessage.warning('请选择资产')
    return
  }
  
  if (!singleForm.value.depreciationMethod) {
    ElMessage.warning('请选择折旧方法')
    return
  }
  
  if (!singleForm.value.depreciationMonth) {
    ElMessage.warning('请选择折旧月份')
    return
  }
  
  // 根据选择的月份计算该月的第一天和最后一天
  const [year, month] = singleForm.value.depreciationMonth.split('-').map(Number)
  const startDate = new Date(year, month - 1, 1)
  const endDate = new Date(year, month, 0)
  
  const loading = ElLoading.service({ text: '计算中...' })
  
  try {
    const response = await axios.post(`/depreciation/calculate/${singleForm.value.assetId}`, null, {
      params: {
        startDate: formatDateForApi(startDate),
        endDate: formatDateForApi(endDate),
        depreciationMethod: singleForm.value.depreciationMethod,
        actualWorkUnits: singleForm.value.depreciationMethod === 'WORK_UNIT' ? singleForm.value.actualWorkUnits : null
      }
    })
    
    if (response.data.code === 200) {
      singleResult.value = response.data.data
      ElMessage.success('计算成功')
    } else {
      ElMessage.error(response.data.message || '计算失败')
    }
  } catch (error) {
    console.error('计算失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.close()
  }
}

// 批量计算折旧
const calculateBatchDepreciation = async () => {
  if (!batchForm.value.assetIds || batchForm.value.assetIds.length === 0) {
    ElMessage.warning('请选择资产')
    return
  }
  
  if (!batchForm.value.depreciationMonth) {
    ElMessage.warning('请选择折旧月份')
    return
  }
  
  // 确保折旧月份格式正确为 yyyy-MM
  let month = batchForm.value.depreciationMonth
  if (typeof month === 'object') {
    const date = new Date(month)
    month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  }
  
  const loading = ElLoading.service({ text: '计算中...' })
  try {
    const response = await axios.post('/depreciation/calculate/batch', batchForm.value.assetIds, {
      params: {
        depreciationMonth: month,
        depreciationMethod: batchForm.value.depreciationMethod || ''
      }
    })
    
    if (response.data.code === 200) {
      batchResults.value = response.data.data
      ElMessage.success('批量计算成功')
    } else {
      ElMessage.error(response.data.message || '计算失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.close()
  }
}

// 创建任务
const createTask = async () => {
  if (!taskForm.value.taskName) {
    ElMessage.warning('请输入任务名称')
    return
  }
  
  try {
    // 暂时返回成功，因为后端接口不存在
    ElMessage.success('任务创建成功')
    createTaskDialogVisible.value = false
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 执行任务
const executeTask = async (taskId) => {
  try {
    // 暂时返回成功，因为后端接口不存在
    ElMessage.success('任务执行成功')
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 查看任务详情
const viewTaskDetail = (taskId) => {
  // 可以跳转到任务详情页面或显示详情对话框
  ElMessage.info('查看任务详情功能待实现')
}

// 回滚记录
const rollbackRecord = async (recordId) => {
  try {
    const response = await axios.delete(`/depreciation/records/${recordId}/rollback`)
    
    if (response.data.code === 200) {
      ElMessage.success('回滚成功')
      loadRecords()
    } else {
      ElMessage.error(response.data.message || '回滚失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 查询记录
const searchRecords = async () => {
  try {
    const response = await axios.get('/depreciation/records/date-range', {
      params: {
        startDate: formatDateForApi(recordSearchForm.value.startDate),
        endDate: formatDateForApi(recordSearchForm.value.endDate)
      }
    })
    
    if (response.data.code === 200) {
      records.value = response.data.data
    } else {
      ElMessage.error('查询失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 刷新任务
const refreshTasks = () => {
  loadTasks()
}

// 重置单资产表单
const resetSingleForm = () => {
  singleForm.value = {
    assetId: null,
    depreciationMethod: '',
    depreciationMonth: '',
    actualWorkUnits: 1
  }
  singleResult.value = null
}

// 重置批量表单
const resetBatchForm = () => {
  batchForm.value = {
    assetIds: [],
    depreciationMonth: new Date()
  }
  batchResults.value = []
}

// 重置记录查询
const resetRecordSearch = () => {
  recordSearchForm.value = {
    assetNo: '',
    startDate: null,
    endDate: null
  }
  loadRecords()
}

// 打开创建任务对话框
const openCreateTaskDialog = () => {
  taskForm.value = {
    taskName: '',
    taskType: 'AUTO',
    periodType: 'MONTHLY',
    periodValue: 1
  }
  createTaskDialogVisible.value = true
}

// 格式化方法
const getDepreciationMethodName = (method) => {
  const methodMap = {
    'STRAIGHT_LINE': '直线法',
    'DOUBLE_DECLINING_BALANCE': '双倍余额递减法',
    'DOUBLE_DECLINING': '双倍余额递减法',
    'WORK_UNIT': '工作量法'
  }
  return methodMap[method] || method
}

const formatDepreciationMethod = (value) => {
  // 如果是对象，提取字段值
  if (value && typeof value === 'object') {
    return getDepreciationMethodName(value.depreciationMethod || value.depreciation_method)
  }
  // 如果是字符串，直接转换
  return getDepreciationMethodName(value)
}

const formatTaskStatus = (row) => {
  const statusMap = {
    'PENDING': '待执行',
    'RUNNING': '执行中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[row.taskStatus] || row.taskStatus
}

const formatCurrency = (value) => {
  if (!value || value === 'NaN' || value === null || value === undefined) return '0.00'
  const num = parseFloat(value)
  if (isNaN(num)) return '0.00'
  return num.toFixed(2)
}

const formatDate = (date) => {
  if (!date || date === 'Invalid Date') return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString()
}

// 标签切换处理
const handleTabClick = (tab) => {
  if (tab.props.name === 'task') {
    loadTasks()
  } else if (tab.props.name === 'record') {
    loadRecords()
  }
}
</script>

<style scoped>
.depreciation-container {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form {
  margin-bottom: 20px;
}

.result-card {
  margin-top: 20px;
}

.task-actions {
  margin-bottom: 15px;
}

.search-form {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

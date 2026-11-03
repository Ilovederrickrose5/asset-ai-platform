<template>
  <div class="eam-page asset-list-page">
    <PageHeader :title="pageTitle" :subtitle="pageSubtitle" eyebrow="ASSETS">
      <template #actions>
        <el-button v-permission="['admin', 'manager']" type="primary" @click="handleAdd" :icon="Plus">
          新增资产
        </el-button>
      </template>
    </PageHeader>

    <div class="eam-page-body">
      <!-- 筛选 -->
      <section class="eam-panel filter-panel">
        <div class="eam-panel__body filter-body">
          <el-form :model="searchForm" class="filter-form" @submit.prevent="handleSearch">
            <el-form-item label="资产名称">
              <el-input
                v-model="searchForm.assetName"
                placeholder="搜索名称或编号"
                clearable
                :prefix-icon="Search"
              />
            </el-form-item>
            <el-form-item label="资产状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px">
                <el-option label="在库" value="in_stock" />
                <el-option label="使用中" value="using" />
                <el-option label="维修中" value="maintenance" />
                <el-option label="已报废" value="scrapped" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </el-form>
        </div>
      </section>

      <!-- 资产列表 -->
      <section class="eam-panel">
        <div class="eam-panel__head">
          <div class="eam-panel__title">
            <span class="eam-panel__icon"><el-icon><Files /></el-icon></span>
            <div>
              <strong>资产清单</strong>
              <span>共 {{ total }} 项资产 · 数据实时同步</span>
            </div>
          </div>
          <span class="eam-panel__count">第 {{ currentPage }} 页</span>
        </div>

        <div class="eam-panel__body table-body">
      <el-table v-if="!isMobile" :data="filteredAssetList" v-loading="loading" class="asset-table">
        <el-table-column type="index" label="序号" width="52" />
        <el-table-column prop="assetNo" label="资产编号" width="178">
          <template #default="scope">
            <span class="mono">{{ scope.row.assetNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="assetName" label="资产名称" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="showDetailColumns" prop="categoryName" label="分类" width="96" show-overflow-tooltip />
        <el-table-column v-if="showModelColumn" prop="model" label="型号" min-width="96" show-overflow-tooltip />
        <el-table-column v-if="showDetailColumns" prop="userName" label="使用人" width="100" show-overflow-tooltip />
        <el-table-column label="存放位置" width="150">
          <template #default="scope">
            <div class="location-cell">
              <span class="loc-text">{{ scope.row.location || '-' }}</span>
              <template v-if="scope.row.borrowStatus === 'borrowed'">
                <span class="borrow-chip">已借出</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div class="borrow-tooltip">
                      <div><strong>当前位置：</strong>{{ scope.row.currentLocation || '未知' }}</div>
                      <div><strong>借用人：</strong>{{ scope.row.borrowerName || '未知' }}</div>
                      <div><strong>借出时间：</strong>{{ formatTime(scope.row.borrowTime) }}</div>
                      <div v-if="scope.row.expectedReturnTime">
                        <strong>预计归还：</strong>{{ formatTime(scope.row.expectedReturnTime) }}
                      </div>
                    </div>
                  </template>
                  <el-icon class="info-icon"><InfoFilled /></el-icon>
                </el-tooltip>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="资产状态" width="118">
          <template #default="scope">
            <StatusTag :status="scope.row.status" :status-map="assetStatusMap" />
          </template>
        </el-table-column>

        <template v-if="showStaffColumns">
          <el-table-column prop="purchasePrice" label="采购价格" width="110" align="right">
            <template #default="scope">
              <span class="money">{{ formatPrice(scope.row.purchasePrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="showSupplierColumn" label="供应商" min-width="96" show-overflow-tooltip>
            <template #default="scope">
              {{ getSupplierName(scope.row.supplierId) }}
            </template>
          </el-table-column>
        </template>

        <el-table-column v-if="showStaffColumns && canManageAssets" label="操作" width="172" fixed="right">
          <template #default="scope">
            <div class="row-actions">
              <el-button link class="act act--primary" @click="handleEdit(scope.row)">编辑</el-button>
              <span class="act-sep" aria-hidden="true"></span>
              <el-button link class="act" @click="handleUpdateStatus(scope.row)">状态</el-button>
              <span class="act-sep" aria-hidden="true"></span>
              <el-button link class="act act--danger" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="!showStaffColumns" label="操作" width="172" fixed="right">
          <template #default="scope">
            <div class="row-actions">
              <el-button
                link
                class="act act--primary"
                :disabled="scope.row.status === 'using' || scope.row.status === 'scrapped'"
                @click="handleApplyAsset(scope.row)"
              >
                申请
              </el-button>
              <template v-if="isAssetUserOrBorrower(scope.row)">
                <template v-if="scope.row.status === 'maintenance'">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleRepairComplete(scope.row)">维修完成</el-button>
                </template>
                <template v-else-if="hasApprovedMaintenance(scope.row.id)">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleStartMaintenance(scope.row)">开始维修</el-button>
                </template>
                <template v-else-if="(scope.row.status === 'using' || scope.row.borrowStatus === 'borrowed') && !hasPendingMaintenance(scope.row.id)">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleApplyMaintenance(scope.row)">申请维修</el-button>
                </template>
                <template v-else-if="hasPendingMaintenance(scope.row.id)">
                  <span class="act-note">申请已提交</span>
                </template>
              </template>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="table-empty">
            <span class="empty-icon"><el-icon><Files /></el-icon></span>
            <p class="empty-text">暂无资产记录</p>
            <p class="empty-hint">调整筛选条件或新增资产后再试</p>
          </div>
        </template>
        </el-table>

        <div v-if="isMobile" v-loading="loading" class="asset-cards">
          <article
            v-for="asset in filteredAssetList"
            :key="asset.id"
            class="asset-card-item"
          >
            <div class="asset-card-item__top">
              <div class="asset-card-item__title">
                <span class="mono">{{ asset.assetNo }}</span>
                <strong>{{ asset.assetName }}</strong>
              </div>
              <StatusTag :status="asset.status" :status-map="assetStatusMap" />
            </div>

            <dl class="asset-card-item__meta">
              <div><dt>分类</dt><dd>{{ asset.categoryName }}</dd></div>
              <div><dt>型号</dt><dd>{{ asset.model || '-' }}</dd></div>
              <div><dt>使用人</dt><dd>{{ asset.userName }}</dd></div>
              <div><dt>位置</dt><dd>{{ asset.location || '-' }}</dd></div>
              <div v-if="showStaffColumns"><dt>采购价</dt><dd>{{ formatPrice(asset.purchasePrice) }}</dd></div>
              <div v-if="asset.borrowStatus === 'borrowed'"><dt>借出</dt><dd>已借出</dd></div>
            </dl>

            <div v-if="canManageAssets" class="asset-card-item__foot">
              <el-button link class="act act--primary" @click="handleEdit(asset)">编辑</el-button>
              <span class="act-sep" aria-hidden="true"></span>
              <el-button link class="act" @click="handleUpdateStatus(asset)">状态</el-button>
              <span class="act-sep" aria-hidden="true"></span>
              <el-button link class="act act--danger" @click="handleDelete(asset)">删除</el-button>
            </div>

            <div v-else-if="!showStaffColumns" class="asset-card-item__foot">
              <el-button
                link
                class="act act--primary"
                :disabled="asset.status === 'using' || asset.status === 'scrapped'"
                @click="handleApplyAsset(asset)"
              >
                申请
              </el-button>
              <template v-if="isAssetUserOrBorrower(asset)">
                <template v-if="asset.status === 'maintenance'">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleRepairComplete(asset)">维修完成</el-button>
                </template>
                <template v-else-if="hasApprovedMaintenance(asset.id)">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleStartMaintenance(asset)">开始维修</el-button>
                </template>
                <template v-else-if="(asset.status === 'using' || asset.borrowStatus === 'borrowed') && !hasPendingMaintenance(asset.id)">
                  <span class="act-sep" aria-hidden="true"></span>
                  <el-button link class="act" @click="handleApplyMaintenance(asset)">申请维修</el-button>
                </template>
                <template v-else-if="hasPendingMaintenance(asset.id)">
                  <span class="act-note">申请已提交</span>
                </template>
              </template>
            </div>
          </article>

          <div v-if="!filteredAssetList.length && !loading" class="table-empty">
            <span class="empty-icon"><el-icon><Files /></el-icon></span>
            <p class="empty-text">暂无资产记录</p>
            <p class="empty-hint">调整筛选条件或新增资产后再试</p>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
        </div>
      </section>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      width="720px"
      :close-on-click-modal="false"
      align-center
      class="eam-dialog asset-dialog"
    >
      <template #header>
        <div class="dialog-head">
          <div class="dialog-head__text">
            <strong>{{ dialogTitle }}</strong>
            <span>{{ isEdit ? '修改资产信息后保存，列表数据将同步刷新' : '完善资产基础信息与采购信息后入库' }}</span>
          </div>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="96px" class="asset-form">
        <!-- 基本信息 -->
        <div class="form-section"><span>基本信息</span></div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产编号" prop="assetNo" required>
              <el-input v-model="form.assetNo" placeholder="系统自动生成" :disabled="!isEdit" />
              <el-button v-if="!isEdit" link type="primary" size="small" class="generate-link" @click="generateAssetNo">
                自动生成编号
              </el-button>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName" required>
              <el-input v-model="form.assetName" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产分类" prop="categoryId" required>
              <el-select v-model="form.categoryId" placeholder="请选择资产分类" style="width: 100%">
                <el-option
                  v-for="category in categoryOptions"
                  :key="category.id"
                  :label="category.categoryName"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select 
                v-model="form.supplierId" 
                placeholder="请选择或输入供应商" 
                clearable 
                filterable 
                allow-create 
                default-first-option
                style="width: 100%"
                @change="handleSupplierChange"
                @visible-change="handleSupplierVisibleChange"
              >
                <el-option
                  v-for="supplier in supplierOptions"
                  :key="supplier.id"
                  :label="supplier.supplierName"
                  :value="supplier.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="型号" prop="model" required>
              <el-input v-model="form.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit" required>
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 采购信息 -->
        <div class="form-section"><span>采购信息</span></div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购价格" prop="purchasePrice" required>
              <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate" required>
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="保修期(月)" prop="warrantyPeriod" required>
              <el-input-number v-model="form.warrantyPeriod" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用年限(月)" prop="usefulLife" required>
              <el-input-number v-model="form.usefulLife" :min="1" :step="12" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 状态信息 -->
        <div class="form-section"><span>状态信息</span></div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产状态" prop="status" required>
              <el-select v-model="form.status" placeholder="请选择资产状态" style="width: 100%" :disabled="!isEdit">
                <el-option label="在库" value="in_stock" />
                <el-option label="使用中" value="using" />
                <el-option label="维修中" value="maintenance" />
                <el-option label="已报废" value="scrapped" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用状态" prop="useStatus" required>
              <el-select v-model="form.useStatus" placeholder="请选择使用状态" style="width: 100%" :disabled="!isEdit">
                <el-option label="闲置" value="idle" />
                <el-option label="使用中" value="using" />
                <el-option label="维修中" value="repairing" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="存放位置" prop="location" required>
              <el-autocomplete
                v-model="form.location"
                :fetch-suggestions="querySearch"
                placeholder="请输入或选择存放位置"
                clearable
                style="width: 100%"
                @select="handleLocationSelect"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 借出信息 -->
        <div class="form-section"><span>借出信息</span></div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="借出状态" prop="borrowStatus" required>
              <el-select v-model="form.borrowStatus" placeholder="请选择借出状态" style="width: 100%" :disabled="!isEdit" @change="handleBorrowStatusChange">
                <el-option label="未借出" value="not_borrowed" />
                <el-option label="已借出" value="borrowed" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 已借出时显示的额外字段 -->
        <template v-if="form.borrowStatus === 'borrowed'">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="当前位置" prop="currentLocation">
                <el-input v-model="form.currentLocation" placeholder="请输入当前实际位置" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="借用人" prop="borrowerId">
                <el-select v-model="form.borrowerId" placeholder="请选择借用人" clearable style="width: 100%" @change="handleBorrowerChange">
                  <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="user.realName || user.username"
                    :value="user.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="借出时间" prop="borrowTime">
                <el-date-picker 
                  v-model="form.borrowTime" 
                  type="datetime" 
                  placeholder="选择借出时间" 
                  style="width: 100%" 
                  value-format="YYYY-MM-DD HH:mm:ss" 
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计归还" prop="expectedReturnTime">
                <el-date-picker 
                  v-model="form.expectedReturnTime" 
                  type="datetime" 
                  placeholder="选择预计归还时间" 
                  style="width: 100%" 
                  value-format="YYYY-MM-DD HH:mm:ss" 
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        
        <!-- 备注信息 -->
        <div class="form-section"><span>备注信息</span></div>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 更新状态对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      width="440px"
      :close-on-click-modal="false"
      align-center
      class="eam-dialog"
    >
      <template #header>
        <div class="dialog-head">
          <div class="dialog-head__text">
            <strong>更新资产状态</strong>
            <span>{{ currentAsset?.assetName || '调整资产状态，保存后立即生效' }}</span>
          </div>
        </div>
      </template>
      <el-form :model="statusForm" label-position="top" class="status-form">
        <el-form-item label="资产状态">
          <el-select v-model="statusForm.status" placeholder="请选择资产状态" style="width: 100%">
            <el-option label="在库" value="in_stock" />
            <el-option label="使用中" value="using" />
            <el-option label="维修中" value="maintenance" />
            <el-option label="已报废" value="scrapped" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="statusDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleStatusSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import axios from '../utils/request'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/userStore'
import {
  Plus,
  Search,
  InfoFilled,
  Files
} from '@element-plus/icons-vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'

export default {
  name: 'AssetList',
  components: { PageHeader, StatusTag },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const userStore = useUserStore()
    const loading = ref(false)
    const assetList = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const onlyMine = computed(() => route.query.onlyMyAssets === 'true')
    const categoryOptions = ref([])
    const departmentOptions = ref([])
    const userOptions = ref([])
    const supplierOptions = ref([])
    const locationOptions = ref([])
    
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const formRef = ref(null)
    const isEdit = ref(false)
    const currentId = ref(null)

    const statusDialogVisible = ref(false)
    const statusForm = reactive({
      status: ''
    })
    const currentAsset = ref(null)

    const searchForm = reactive({
      assetName: '',
      status: ''
    })

    const form = reactive({
      assetNo: '',
      assetName: '',
      categoryId: null,
      supplierId: null,
      model: '',
      unit: '',
      purchasePrice: 0,
      originalValue: 0,
      netValue: 0,
      purchaseDate: '',
      warrantyPeriod: 12,
      usefulLife: 60,
      status: 'in_stock',
      useStatus: 'idle',
      location: '',
      userId: null,
      remark: '',
      borrowStatus: 'not_borrowed',
      currentLocation: '',
      borrowerId: null,
      borrowerName: '',
      borrowTime: '',
      expectedReturnTime: ''
    })

    const rules = reactive({
      assetNo: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
      assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
      categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
      model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
      warrantyPeriod: [{ required: true, message: '请输入保修期', trigger: 'blur' }],
      unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
      purchasePrice: [{ required: true, message: '请输入采购价格', trigger: 'blur' }],
      purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'blur' }],
      usefulLife: [{ required: true, message: '请输入使用年限', trigger: 'blur' }],
      status: [{ required: true, message: '请选择资产状态', trigger: 'change' }],
      useStatus: [{ required: true, message: '请选择使用状态', trigger: 'change' }]
    })

    const getToken = () => userStore.token

    // 获取用户角色
    const getUserRole = () => {
      return userStore.currentUserRole
    }
    
    // 页面标题（根据角色动态显示）
    const pageTitle = computed(() => {
      if (onlyMine.value) {
        return '我的资产'
      }
      const userRole = getUserRole()
      const titleMap = {
        'admin': '资产列表',
        'leader': '部门资产',
        'manager': '部门资产',
        'user': '资产列表'
      }
      return titleMap[userRole] || '资产列表'
    })

    const showStaffColumns = computed(() => userStore.hasRole(['admin', 'leader', 'manager']))
    const canManageAssets = computed(() => userStore.hasRole(['admin', 'manager']))

    const viewportWidth = ref(window.innerWidth)
    const isMobile = computed(() => viewportWidth.value <= 1024)
    const showDetailColumns = computed(() => viewportWidth.value > 1180)
    const showModelColumn = computed(() => viewportWidth.value > 1280)
    const showSupplierColumn = computed(() => viewportWidth.value > 1360)

    const handleViewportResize = () => {
      viewportWidth.value = window.innerWidth
    }

    const pageSubtitle = computed(() => {
      if (onlyMine.value) {
        return '查看名下资产，可发起维修等申请'
      }
      return showStaffColumns.value
        ? '管理资产台账、使用状态与借出情况，支持按名称与状态筛选'
        : '浏览公司全部资产，可对闲置资产发起领用、报修等申请'
    })

    const assetStatusMap = {
      in_stock: { text: '在库', type: 'info' },
      using: { text: '使用中', type: 'success' },
      maintenance: { text: '维修中', type: 'warning' },
      scrapped: { text: '已报废', type: 'danger' }
    }

    // 获取资产列表（分页与筛选均由后端处理）
    const fetchAssets = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value,
          sortBy: 'id',
          sortDir: 'desc'
        }
        if (searchForm.status) {
          params.status = searchForm.status
        }
        if (searchForm.assetName) {
          params.keyword = searchForm.assetName.trim()
        }
        if (onlyMine.value) {
          params.onlyMine = true
        }
        const response = await axios.get('/assets', { params })
        if (response.data.code === 200) {
          const data = response.data.data
          assetList.value = data.content || data || []
          total.value = data.totalElements ?? assetList.value.length
          // 更新存放位置列表
          fetchLocations()
        }
      } catch (error) {
        console.error('获取资产列表失败', error)
        ElMessage.error('获取资产列表失败')
      } finally {
        loading.value = false
      }
    }

    // 获取资产分类列表
    const fetchCategories = async () => {
      try {
        const response = await axios.get('/asset-categories')
        if (response.data.code === 200) {
          const allCategories = response.data.data
          // 过滤出最小级别的分类（没有子分类的分类）
          const leafCategories = allCategories.filter(category => {
            return !allCategories.some(c => c.parentId === category.id)
          })
          categoryOptions.value = leafCategories
        }
      } catch (error) {
        console.error('获取资产分类失败', error)
      }
    }

    // 获取部门列表
    const fetchDepartments = async () => {
      try {
        const response = await axios.get('/departments')
        if (response.data.code === 200) {
          departmentOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取部门列表失败', error)
      }
    }

    // 获取用户列表
    const fetchUsers = async () => {
      try {
        const response = await axios.get('/users')
        if (response.data.code === 200) {
          userOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取用户列表失败', error)
      }
    }

    // 获取供应商列表
    const fetchSuppliers = async () => {
      try {
        const response = await axios.get('/suppliers')
        if (response.data.code === 200) {
          supplierOptions.value = response.data.data
        }
      } catch (error) {
        console.error('获取供应商列表失败', error)
      }
    }

    // 获取存放位置列表（跨页累积，作为自动补全候选）
    const fetchLocations = () => {
      const locations = new Set(locationOptions.value)
      assetList.value.forEach(asset => {
        if (asset.location) {
          locations.add(asset.location)
        }
      })
      locationOptions.value = Array.from(locations)
    }

    // 资产列表展示数据（权限范围、搜索和分页由后端完成，这里只做名称补全）
    const filteredAssetList = computed(() => {
      return assetList.value.map(asset => {
        const category = categoryOptions.value.find(c => c.id === asset.categoryId)
        const department = departmentOptions.value.find(d => d.id === asset.deptId)
        const user = userOptions.value.find(u => u.id === asset.userId)
        const supplierId = Number(asset.supplierId)
        const supplier = supplierOptions.value.find(s => Number(s.id) === supplierId)
        return {
          ...asset,
          categoryName: category ? category.categoryName : '-',
          departmentName: department ? department.deptName : '-',
          userName: user ? (user.realName || user.username) : '-',
          supplierName: supplier ? supplier.supplierName : '-'
        }
      })
    })

    // 获取供应商名称
    const getSupplierName = (supplierId) => {
      if (!supplierId) return '-'
      // 确保类型匹配，将supplierId转换为数字
      const id = Number(supplierId)
      const supplier = supplierOptions.value.find(s => Number(s.id) === id)
      return supplier ? supplier.supplierName : '-'
    }

    // 格式化价格
    const formatPrice = (price) => {
      if (price === null || price === undefined) return '-'
      const amount = parseFloat(price)
      if (!Number.isFinite(amount)) return '-'
      return '¥' + amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }

    // 检查当前用户是否是资产的使用者或借用者
    const isAssetUserOrBorrower = (asset) => {
      const currentUserId = userStore.userId
      if (currentUserId == null) return false

      // 检查是否是资产的使用者（类型转换确保比较正确）
      if (Number(asset.userId) === Number(currentUserId)) {
        return true
      }

      // 检查是否是资产的借用者（类型转换确保比较正确）
      if (asset.borrowStatus === 'borrowed' && Number(asset.borrowerId) === Number(currentUserId)) {
        return true
      }
      return false
    }

    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return new Date(time).toLocaleString()
    }

    // 生成资产编号
    const generateAssetNo = () => {
      // 获取当前日期
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      
      // 生成随机数，确保唯一性
      const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
      
      // 构建资产编号：AST-YYYY-MM-XXXX
      const assetNo = `AST-${year}-${month}-${random}`
      
      // 检查编号是否已存在
      const isExists = assetList.value.some(asset => asset.assetNo === assetNo)
      
      if (isExists) {
        // 如果存在，递归生成新编号
        generateAssetNo()
      } else {
        form.assetNo = assetNo
        ElMessage.success('资产编号生成成功')
      }
    }

    // 申请资产
    const handleApplyAsset = (row) => {
      router.push({ path: '/assets/apply', query: { assetId: row.id } })
    }

    // 搜索
    const handleSearch = () => {
      currentPage.value = 1
      fetchAssets()
    }

    // 重置搜索
    const handleReset = () => {
      searchForm.assetName = ''
      searchForm.status = ''
      currentPage.value = 1
      fetchAssets()
    }

    // 分页
    const handleSizeChange = (size) => {
      pageSize.value = size
      currentPage.value = 1
      fetchAssets()
    }

    const handleCurrentChange = (page) => {
      currentPage.value = page
      fetchAssets()
    }

    // 新增资产
    const handleAdd = () => {
      isEdit.value = false
      currentId.value = null
      dialogTitle.value = '新增资产'
      Object.assign(form, {
        assetNo: '',
        assetName: '',
        categoryId: null,
        supplierId: null,
        model: '',
        unit: '',
        purchasePrice: 0,
        originalValue: 0,
        netValue: 0,
        purchaseDate: '',
        warrantyPeriod: 12,
        usefulLife: 60,
        status: 'in_stock', // 默认在库
        useStatus: 'idle', // 默认闲置
        location: '',
        userId: null,
        remark: '',
        // 借出信息默认值
        borrowStatus: 'not_borrowed', // 默认未借出
        currentLocation: '',
        borrowerId: null,
        borrowerName: '',
        borrowTime: '',
        expectedReturnTime: ''
      })
      dialogVisible.value = true
    }

    // 编辑资产
    const handleEdit = (row) => {
      isEdit.value = true
      currentId.value = row.id
      dialogTitle.value = '编辑资产'
      Object.assign(form, {
        assetNo: row.assetNo,
        assetName: row.assetName,
        categoryId: row.categoryId,
        supplierId: row.supplierId,
        model: row.model || '',
        unit: row.unit || '',
        purchasePrice: row.purchasePrice || 0,
        originalValue: row.originalValue || row.purchasePrice || 0,
        netValue: row.netValue || row.purchasePrice || 0,
        purchaseDate: row.purchaseDate || '',
        warrantyPeriod: row.warrantyPeriod || 12,
        usefulLife: row.usefulLife || 60,
        status: row.status || 'in_stock',
        useStatus: row.useStatus || 'idle',
        location: row.location || '',
        userId: row.userId,
        remark: row.remark || '',
        // 借出信息
        borrowStatus: row.borrowStatus || 'not_borrowed',
        currentLocation: row.currentLocation || '',
        borrowerId: row.borrowerId,
        borrowerName: row.borrowerName || '',
        borrowTime: row.borrowTime || '',
        expectedReturnTime: row.expectedReturnTime || ''
      })
      dialogVisible.value = true
    }

    // 借出状态改变处理
    const handleBorrowStatusChange = (value) => {
      if (value === 'not_borrowed') {
        // 清空借出相关信息
        form.currentLocation = ''
        form.borrowerId = null
        form.borrowerName = ''
        form.borrowTime = ''
        form.expectedReturnTime = ''
      } else if (value === 'borrowed') {
        // 设置默认借出时间为当前时间
        if (!form.borrowTime) {
          const now = new Date()
          form.borrowTime = now.toISOString().slice(0, 19).replace('T', ' ')
        }
      }
    }

    // 借用人改变处理
    const handleBorrowerChange = (userId) => {
      if (userId) {
        const user = userOptions.value.find(u => u.id === userId)
        if (user) {
          form.borrowerName = user.realName || user.username
        }
      } else {
        form.borrowerName = ''
      }
    }

    // 供应商改变处理
    const handleSupplierChange = async (value) => {
      // 如果是字符串，说明是手动输入的新供应商
      if (typeof value === 'string') {
        try {
          // 创建新供应商
          const response = await axios.post('/suppliers', {
            supplierName: value
          })
          if (response.data.code === 200) {
            const newSupplier = response.data.data
            // 更新供应商选项
            supplierOptions.value.push(newSupplier)
            // 更新表单中的供应商ID
            form.supplierId = newSupplier.id
            ElMessage.success('供应商创建成功')
          }
        } catch (error) {
          console.error('创建供应商失败', error)
          ElMessage.error('创建供应商失败，请重试')
        }
      }
    }

    // 供应商下拉框显示/隐藏处理
    const handleSupplierVisibleChange = (visible) => {
      if (visible) {
        // 下拉框显示时，确保供应商列表是最新的
        fetchSuppliers()
      }
    }

    // 存放位置变化处理
    const handleLocationChange = (value) => {
      // 确保值被正确设置到 form.location，处理可能的数组情况
      if (value) {
        // 如果 value 是数组，取第一个元素
        if (Array.isArray(value)) {
          form.location = value[0]
        } else {
          form.location = value
        }
      }
    }

    // 存放位置失去焦点处理
    const handleLocationBlur = () => {
      // 当失去焦点时，确保值被正确设置
      // 这里可以添加额外的验证逻辑
    }

    // 存放位置搜索处理
    const querySearch = (queryString, callback) => {
      // 过滤出匹配的存放位置
      const results = queryString ? locationOptions.value.filter(location => {
        return location.toLowerCase().includes(queryString.toLowerCase())
      }) : locationOptions.value
      // 转换为 el-autocomplete 需要的格式
      const formattedResults = results.map(location => ({
        value: location,
        label: location
      }))
      callback(formattedResults)
    }

    // 存放位置选择处理
    const handleLocationSelect = (item) => {
      // 确保值被正确设置
      form.location = item.value
    }

    // 更新状态
    const handleUpdateStatus = (row) => {
      currentAsset.value = row
      statusForm.status = row.status
      statusDialogVisible.value = true
    }

    // 提交状态更新
    const handleStatusSubmit = async () => {
      try {
        // 更新资产状态
        if (statusForm.status !== currentAsset.value.status) {
          const response = await axios.put(`/assets/${currentAsset.value.id}/status`, 
            { status: statusForm.status }
          )
          if (response.data.code !== 200) {
            ElMessage.error('更新资产状态失败')
            return
          }
        }
        
        ElMessage.success('状态更新成功')
        statusDialogVisible.value = false
        fetchAssets()
      } catch (error) {
        ElMessage.error('状态更新失败')
      }
    }

    // 删除资产
    const handleDelete = (row) => {
      ElMessageBox.confirm(`确定删除资产 "${row.assetName}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.delete(`/assets/${row.id}`)
          if (response.data.code === 200) {
            ElMessage.success('删除成功')
            fetchAssets()
          } else {
            ElMessage.error(response.data.message || '删除失败')
          }
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    // 提交表单
    const handleSubmit = async () => {
      formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            let response
            if (isEdit.value) {
              response = await axios.put(`/assets/${currentId.value}`, form)
            } else {
              response = await axios.post('/assets', form)
            }
            if (response.data.code === 200) {
              ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
              dialogVisible.value = false
              fetchAssets()
              // 更新存放位置列表
              fetchLocations()
            } else {
              ElMessage.error(response.data.message || '操作失败')
            }
          } catch (error) {
            ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
          }
        }
      })
    }

    // 维修完成处理函数
    const handleRepairComplete = async (row) => {
      try {
        const currentUser = userStore.user
        if (!currentUser) {
          ElMessage.error('请先登录')
          return
        }

        // 找到对应的维修中申请
        const inProgressApp = maintenanceApplications.value.find(app => {
          const appAssetId = app.assetId || app.asset_id
          const appStatus = app.status || app.application_status
          return Number(appAssetId) === Number(row.id) && appStatus === 'in_progress'
        })

        if (!inProgressApp) {
          ElMessage.error('未找到维修中的申请记录')
          return
        }

        // 调用完成维修API
        const response = await axios.post(`/asset-applications/${inProgressApp.id}/complete-maintenance`, {
          userId: currentUser.id,
          userName: currentUser.realName || currentUser.username
        }, {
          headers: { Authorization: `Bearer ${getToken()}` }
        })

        if (response.data.code === 200) {
          ElMessage.success('维修完成，资产状态已更新为使用中')
          // 更新资产列表和维修申请列表
          fetchAssets()
          await fetchMaintenanceApplications()
        } else {
          ElMessage.error(response.data.message || '操作失败')
        }
      } catch (error) {
        console.error('维修完成失败:', error)
        ElMessage.error('操作失败，请重试')
      }
    }

    // 维修申请列表（包含所有状态）
    const maintenanceApplications = ref([])

    // 获取维修申请列表
    const fetchMaintenanceApplications = async () => {
      try {
        const currentUser = userStore.user
        if (!currentUser) {
          return
        }
        const response = await axios.get(`/asset-applications/applicant/${currentUser.id}`)
        if (response.data.code === 200) {
          // 后端返回的是Page分页对象，实际数据在content属性中
          const data = response.data.data
          const applications = data.content || data
          maintenanceApplications.value = applications.filter(item => {
            const appType = item.applicationType || item.application_type
            const appAssetId = item.assetId || item.asset_id
            const appStatus = item.status || item.application_status
            return (appType === 'REPAIR' || appType === 'MAINTENANCE') && appAssetId && appStatus
          })
        }
      } catch (error) {
        console.error('获取维修申请失败', error)
      }
    }

    // 检查资产是否有待审批的维修申请（类型转换确保比较正确）
    const hasPendingMaintenance = (assetId) => {
      if (!assetId) {
        return false
      }
      return maintenanceApplications.value.some(app => {
        const appAssetId = app.assetId || app.asset_id
        const appStatus = (app.status || app.application_status || '').toLowerCase()
        return appAssetId && Number(appAssetId) === Number(assetId) && appStatus === 'pending'
      })
    }

    // 检查资产是否有已批准的维修申请（类型转换确保比较正确）
    const hasApprovedMaintenance = (assetId) => {
      if (!assetId) {
        return false
      }
      return maintenanceApplications.value.some(app => {
        const appAssetId = app.assetId || app.asset_id
        const appStatus = (app.status || app.application_status || '').toLowerCase()
        return appAssetId && Number(appAssetId) === Number(assetId) && appStatus === 'approved'
      })
    }

    // 申请维修处理函数
    const handleApplyMaintenance = async (row) => {
      // 显示确认对话框
      ElMessageBox.confirm(
        `确定要为资产「${row.assetName}」申请维修吗？`,
        '确认申请维修',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const currentUser = userStore.user
          if (!currentUser) {
            ElMessage.error('请先登录')
            return
          }

          // 创建维修申请记录
          const maintenanceData = {
            assetId: row.id,
            applicationType: 'REPAIR',
            applicantId: currentUser.id,
            applicantName: currentUser.realName || currentUser.username,
            departmentId: currentUser.departmentId || currentUser.deptId,
            departmentName: currentUser.departmentName,
            applicationReason: '资产维修申请',
            maintenanceType: '日常维护',
            applicationDate: new Date().toISOString(),
            status: 'pending'
          }

          const response = await axios.post('/asset-applications', maintenanceData, {
            headers: { Authorization: `Bearer ${getToken()}` }
          })

          if (response.data.code === 200) {
            ElMessage.success('维修申请已提交，请等待资产管理员审批')
            // 更新维修申请列表
            await fetchMaintenanceApplications()
            fetchAssets()
          } else {
            ElMessage.error(response.data.message || '提交申请失败')
          }
        } catch (error) {
          ElMessage.error('操作失败，请重试')
        }
      }).catch(() => {
        // 用户点击取消
        ElMessage.info('已取消申请')
      })
    }

    // 开始维修处理函数
    const handleStartMaintenance = async (row) => {
      try {
        const currentUser = userStore.user
        if (!currentUser) {
          ElMessage.error('请先登录')
          return
        }

        // 找到对应的已批准申请
        const approvedApp = maintenanceApplications.value.find(app => {
          const appAssetId = app.assetId || app.asset_id
          const appStatus = app.status || app.application_status
          return Number(appAssetId) === Number(row.id) && appStatus === 'approved'
        })

        if (!approvedApp) {
          ElMessage.error('未找到已批准的维修申请')
          return
        }

        // 发送包含 userId 和 userName 的请求体
        const response = await axios.put(`/asset-applications/${approvedApp.id}/start-maintenance`, {
          userId: currentUser.id,
          userName: currentUser.realName || currentUser.username
        }, {
          headers: { Authorization: `Bearer ${getToken()}` }
        })

        if (response.data.code === 200) {
          ElMessage.success('已开始维修，资产状态已更新为维修中')
          // 更新申请列表
          await fetchMaintenanceApplications()
          fetchAssets()
        } else {
          ElMessage.error(response.data.message || '开始维修失败')
        }
      } catch (error) {
        console.error('开始维修失败:', error)
        ElMessage.error('操作失败，请重试')
      }
    }



    onMounted(() => {
      window.addEventListener('resize', handleViewportResize)

      // 读取 URL 参数中的搜索关键词
      if (route.query.keyword) {
        searchForm.assetName = route.query.keyword
      }

      fetchAssets()
      fetchCategories()
      fetchDepartments()
      fetchUsers()
      fetchSuppliers()
      fetchLocations()
      fetchMaintenanceApplications()
    })

    onUnmounted(() => {
      window.removeEventListener('resize', handleViewportResize)
    })

    return {
      userStore,
      loading,
      assetList,
      filteredAssetList,
      currentPage,
      pageSize,
      total,
      handleSizeChange,
      handleCurrentChange,
      categoryOptions,
      departmentOptions,
      userOptions,
      supplierOptions,
      locationOptions,
      dialogVisible,
      dialogTitle,
      formRef,
      form,
      rules,
      searchForm,
      statusDialogVisible,
      statusForm,
      pageTitle,
      pageSubtitle,
      showStaffColumns,
      canManageAssets,
      isMobile,
      showDetailColumns,
      showModelColumn,
      showSupplierColumn,
      assetStatusMap,
      currentAsset,
      isAssetUserOrBorrower,
      isEdit,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleUpdateStatus,
      handleStatusSubmit,
      handleDelete,
      handleSubmit,
      handleApplyAsset,
      handleRepairComplete,
      handleApplyMaintenance,
      handleStartMaintenance,
      hasPendingMaintenance,
      hasApprovedMaintenance,
      generateAssetNo,
      handleSupplierChange,
      handleSupplierVisibleChange,
      handleLocationBlur,
      querySearch,
      handleLocationSelect,
      handleBorrowStatusChange,
      handleBorrowerChange,
      getSupplierName,
      formatPrice,
      formatTime,
      Plus,
      Search,
      InfoFilled,
      Files
    }
  }
}
</script>

<style scoped>
.filter-panel {
  animation-delay: 0.04s;
}

.filter-body {
  padding: 16px 22px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 4px 18px;
}

.filter-form :deep(.el-form-item) {
  margin: 0;
}

.filter-form :deep(.el-form-item__label) {
  padding-right: 10px;
  font-size: 12px;
  color: var(--eam-ink-400);
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.table-body {
  padding: 8px 22px 18px;
}

.mono {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: var(--eam-ink-600);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.asset-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.asset-card-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--eam-line-soft);
  border-radius: var(--eam-radius-md);
  box-shadow: 0 1px 2px rgba(20, 24, 33, 0.04);
}

.asset-card-item__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.asset-card-item__title {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.asset-card-item__title strong {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--eam-ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-card-item__title .mono {
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--eam-ink-300);
}

.asset-card-item__meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 14px;
  margin: 0;
  padding: 12px 0 0;
  border-top: 1px solid var(--eam-line-soft);
}

.asset-card-item__meta div {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
}

.asset-card-item__meta dt {
  flex: 0 0 auto;
  font-size: 11.5px;
  color: var(--eam-ink-300);
}

.asset-card-item__meta dd {
  margin: 0;
  font-size: 12.5px;
  color: var(--eam-ink-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-card-item__foot {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-top: 2px;
}

.money {
  font-weight: 500;
  color: var(--eam-ink-800);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.location-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.loc-text {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--eam-ink-600);
}

.borrow-chip {
  flex: 0 0 auto;
  padding: 1px 7px;
  font-size: 11px;
  line-height: 1.5;
  color: #8a6430;
  background: var(--eam-warning-soft);
  border-radius: var(--eam-radius-pill);
}

.info-icon {
  flex: 0 0 auto;
  font-size: 13px;
  color: var(--eam-ink-300);
  cursor: pointer;
  transition: color var(--eam-duration-fast) var(--eam-ease);
}

.info-icon:hover {
  color: var(--eam-primary);
}

.borrow-tooltip {
  font-size: 12px;
  line-height: 1.8;
}

.borrow-tooltip div {
  margin-bottom: 4px;
}

.borrow-tooltip div:last-child {
  margin-bottom: 0;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.act {
  height: 26px;
  padding: 0 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-500);
  border-radius: 7px;
  transition: color var(--eam-duration-fast) var(--eam-ease),
    background-color var(--eam-duration-fast) var(--eam-ease);
}

.act:hover {
  color: var(--eam-ink-800);
  background: rgba(20, 24, 33, 0.04);
}

.act.is-disabled,
.act.is-disabled:hover {
  color: var(--eam-ink-300);
  background: transparent;
  cursor: not-allowed;
}

.act--primary {
  color: var(--eam-primary-deep);
}

.act--primary:hover {
  color: var(--eam-primary-deep);
  background: var(--eam-primary-soft);
}

.act--danger {
  color: #8c4646;
}

.act--danger:hover {
  color: #8c4646;
  background: var(--eam-danger-soft);
}

.act-sep {
  width: 1px;
  height: 11px;
  background: var(--eam-line);
}

.act-note {
  padding-left: 2px;
  font-size: 11.5px;
  color: var(--eam-ink-300);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid var(--eam-line-soft);
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  grid-column: 1 / -1;
  padding: 38px 0;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-bottom: 4px;
  font-size: 19px;
  color: var(--eam-ink-300);
  background: #f3f5f8;
  border: 1px solid var(--eam-line-soft);
  border-radius: 14px;
}

.empty-text {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--eam-ink-500);
}

.empty-hint {
  margin: 0;
  font-size: 12px;
  color: var(--eam-ink-300);
}

.dialog-head__text strong {
  display: block;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--eam-ink-900);
}

.dialog-head__text span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: var(--eam-ink-400);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.form-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0 14px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--eam-ink-400);
}

.form-section::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--eam-line-soft);
}

.form-section span {
  white-space: nowrap;
}

.asset-form > .form-section:first-child {
  margin-top: 4px;
}

.asset-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.generate-link {
  height: auto;
  margin-top: 6px;
  padding: 0;
  font-size: 12px;
}

.asset-form :deep(.el-form-item__label) {
  font-size: 12.5px;
  color: var(--eam-ink-500);
}

.status-form :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .filter-body {
    padding: 14px 16px;
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100% !important;
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .filter-actions .el-button {
    flex: 1;
  }

  .table-body {
    padding: 6px 12px 16px;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}
</style>

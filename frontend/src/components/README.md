# 组件使用文档

本文档详细介绍了企业资产智能管理平台中的通用组件和业务组件的使用方法。

## 目录

1. [通用组件 (Common Components)](#通用组件)
   - [SearchForm 搜索表单](#searchform)
   - [DataTable 数据表格](#datatable)
   - [FormDialog 表单对话框](#formdialog)
2. [业务组件 (Business Components)](#业务组件)
   - [ManagementPage 管理页面](#managementpage)
   - [ApplyPage 申请页面](#applypage)

---

## 通用组件

### SearchForm

搜索表单组件，用于构建各种搜索条件。

#### Props

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `fields` | `Array` | `[]` | 搜索字段配置数组，必填 |
| `modelValue` | `Object` | `{}` | 表单数据对象 |

#### Fields 配置

```javascript
[
  {
    type: 'input',        // 字段类型: input, select, date, date-range, custom
    prop: 'name',         // 字段属性名
    label: '名称',        // 字段标签
    placeholder: '请输入', // 占位符
    width: '200px'        // 宽度
  },
  {
    type: 'select',
    prop: 'status',
    label: '状态',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]
```

#### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `update:modelValue` | `formData` | 表单数据更新 |
| `search` | `formData` | 点击搜索按钮 |
| `reset` | - | 点击重置按钮 |

#### 使用示例

```vue
<template>
  <SearchForm
    :fields="searchFields"
    v-model="searchForm"
    @search="handleSearch"
    @reset="handleReset"
  />
</template>

<script setup>
const searchFields = [
  { type: 'input', prop: 'name', label: '名称' },
  { type: 'select', prop: 'status', label: '状态', options: [...] }
]

const searchForm = reactive({})

const handleSearch = (form) => {
  console.log('搜索条件:', form)
}
</script>
```

---

### DataTable

数据表格组件，封装了 Element Plus 的 el-table，增加了常用功能。

#### Props

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `data` | `Array` | `[]` | 表格数据 |
| `columns` | `Array` | `[]` | 列配置，必填 |
| `loading` | `Boolean` | `false` | 加载状态 |
| `border` | `Boolean` | `false` | 是否显示边框 |
| `showSelection` | `Boolean` | `false` | 是否显示选择列 |
| `showIndex` | `Boolean` | `true` | 是否显示序号列 |
| `showPagination` | `Boolean` | `false` | 是否显示分页 |
| `total` | `Number` | `0` | 总记录数 |
| `currentPage` | `Number` | `1` | 当前页码 |
| `pageSize` | `Number` | `10` | 每页条数 |

#### Columns 配置

```javascript
[
  { prop: 'name', label: '名称', minWidth: '150' },
  { type: 'status', prop: 'status', label: '状态', width: '100', statusMap: {...} },
  { type: 'time', prop: 'createTime', label: '创建时间', width: '180' },
  { type: 'actions', label: '操作', width: '200' },
  { type: 'custom', prop: 'customField', label: '自定义', minWidth: '200' }
]
```

#### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `selection-change` | `selection` | 选择项变化 |
| `edit` | `row` | 点击编辑按钮 |
| `delete` | `row` | 点击删除按钮 |
| `page-change` | `{ pageSize, currentPage }` | 分页变化 |

#### Slots

| 插槽名 | 参数 | 说明 |
|--------|------|------|
| `actions` | `{ row, index }` | 操作列自定义内容 |
| `column-{prop}` | `{ row, value, index }` | 自定义列内容 |
| `status` | `{ row, value }` | 状态列自定义内容 |
| `time` | `{ row, value }` | 时间列自定义内容 |

#### 使用示例

```vue
<template>
  <DataTable
    :data="tableData"
    :columns="columns"
    :loading="loading"
    show-pagination
    :total="total"
    @edit="handleEdit"
    @delete="handleDelete"
  >
    <template #column-status="{ value }">
      <el-tag :type="value === 1 ? 'success' : 'danger'">
        {{ value === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </DataTable>
</template>

<script setup>
const columns = [
  { prop: 'name', label: '名称', minWidth: '150' },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'actions', label: '操作', width: '200' }
]
</script>
```

---

### FormDialog

表单对话框组件，用于新增/编辑操作。

#### Props

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `modelValue` | `Boolean` | `false` | 对话框可见性 |
| `title` | `String` | `''` | 对话框标题 |
| `width` | `String` | `'500px'` | 对话框宽度 |
| `fields` | `Array` | `[]` | 表单字段配置 |
| `formData` | `Object` | `{}` | 表单数据 |
| `formRules` | `Object` | `{}` | 表单验证规则 |
| `labelWidth` | `String` | `'100px'` | 标签宽度 |
| `isEdit` | `Boolean` | `false` | 是否为编辑模式 |

#### Fields 配置

```javascript
[
  {
    type: 'input',        // input, password, textarea, number, select, radio, date, custom
    prop: 'name',         // 字段属性名
    label: '名称',        // 字段标签
    required: true,       // 是否必填
    placeholder: '请输入', // 占位符
    disabledOnEdit: true  // 编辑时禁用
  },
  {
    type: 'select',
    prop: 'status',
    label: '状态',
    options: [...]
  }
]
```

#### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `update:modelValue` | `visible` | 对话框可见性更新 |
| `submit` | `formData` | 表单提交 |
| `cancel` | - | 点击取消 |
| `closed` | - | 对话框关闭后 |

#### Slots

| 插槽名 | 参数 | 说明 |
|--------|------|------|
| `field-{prop}` | `{ field, form, isEdit }` | 自定义表单字段 |
| `footer` | `{ form, isEdit, submitting }` | 自定义底部按钮 |

#### 使用示例

```vue
<template>
  <FormDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :fields="formFields"
    :form-data="formData"
    :form-rules="formRules"
    :is-edit="isEdit"
    @submit="handleSubmit"
  />
</template>

<script setup>
const formFields = [
  { type: 'input', prop: 'name', label: '名称', required: true },
  { type: 'radio', prop: 'status', label: '状态', options: [...] }
]

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}
</script>
```

---

## 业务组件

### ManagementPage

通用管理页面组件，整合了搜索、表格、表单对话框等功能。

#### Props

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `title` | `String` | `''` | 页面标题，必填 |
| `pageClass` | `String` | `'management'` | 页面类名前缀 |
| `data` | `Array` | `[]` | 表格数据 |
| `loading` | `Boolean` | `false` | 加载状态 |
| `columns` | `Array` | `[]` | 表格列配置，必填 |
| `formFields` | `Array` | `[]` | 表单字段配置 |
| `formRules` | `Object` | `{}` | 表单验证规则 |
| `hasPermission` | `Boolean` | `true` | 是否有操作权限 |
| `showBackButton` | `Boolean` | `true` | 是否显示返回按钮 |
| `showAddButton` | `Boolean` | `true` | 是否显示新增按钮 |
| `showEditButton` | `Boolean` | `true` | 是否显示编辑按钮 |
| `showDeleteButton` | `Boolean` | `true` | 是否显示删除按钮 |
| `dialogWidth` | `String` | `'500px'` | 对话框宽度 |

#### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `add` | - | 点击新增按钮 |
| `edit` | `row` | 点击编辑按钮 |
| `delete` | `row` | 点击删除按钮（已确认） |
| `submit` | `{ isEdit, id, ...formData }` | 表单提交 |
| `search` | `formData` | 搜索 |
| `reset` | - | 重置搜索 |

#### Slots

| 插槽名 | 参数 | 说明 |
|--------|------|------|
| `column-{prop}` | `{ value, row }` | 自定义列内容 |
| `actions` | `{ row }` | 操作列自定义内容 |
| `form-{prop}` | `{ form, isEdit }` | 自定义表单字段 |
| `form-footer` | `{ form, isEdit, submitting }` | 自定义表单底部 |

#### 使用示例

```vue
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
    @submit="handleSubmit"
    ref="managementPageRef"
  >
    <!-- 状态列自定义 -->
    <template #column-status="{ value }">
      <el-tag :type="value === 1 ? 'success' : 'danger'">
        {{ value === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
    
    <!-- 表单字段自定义 -->
    <template #form-departmentId="{ form }">
      <el-select v-model="form.departmentId">
        <el-option
          v-for="dept in departmentOptions"
          :key="dept.id"
          :label="dept.deptName"
          :value="dept.id"
        />
      </el-select>
    </template>
  </ManagementPage>
</template>

<script setup>
const tableColumns = [
  { prop: 'deptName', label: '部门名称', minWidth: '150' },
  { prop: 'deptCode', label: '部门编码', minWidth: '120' },
  { type: 'custom', prop: 'status', label: '状态', width: '100' },
  { type: 'actions', label: '操作', width: '200' }
]

const formFields = [
  { type: 'input', prop: 'deptName', label: '部门名称', required: true },
  { type: 'custom', prop: 'departmentId', label: '所属部门' }
]

const handleSubmit = async ({ isEdit, id, ...formData }) => {
  // 提交表单数据
}
</script>
```

---

### ApplyPage

通用申请页面组件，用于申请类页面（资产申请、转移、维修、报废等）。

#### Props

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `pageClass` | `String` | `'asset'` | 页面类名前缀 |
| `formTitle` | `String` | `''` | 表单卡片标题 |
| `recordTitle` | `String` | `'我的申请记录'` | 记录卡片标题 |
| `showForm` | `Boolean` | `true` | 是否显示表单 |
| `records` | `Array` | `[]` | 记录数据 |
| `recordColumns` | `Array` | `[]` | 记录表格列配置，必填 |
| `loading` | `Boolean` | `false` | 加载状态 |
| `showPagination` | `Boolean` | `false` | 是否显示分页 |
| `total` | `Number` | `0` | 总记录数 |

#### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `form-submit` | `formData` | 表单提交 |
| `form-reset` | - | 表单重置 |
| `page-change` | `{ pageSize, currentPage }` | 分页变化 |

#### Slots

| 插槽名 | 参数 | 说明 |
|--------|------|------|
| `form` | `{ submit, reset }` | 表单区域自定义内容 |
| `column-{prop}` | `{ row, value }` | 自定义列内容 |
| `record-actions` | `{ row }` | 记录操作列自定义内容 |
| `dialogs` | - | 额外对话框 |

#### 使用示例

```vue
<template>
  <ApplyPage
    page-class="asset-apply"
    form-title="资产领用申请"
    record-title="我的申请记录"
    :records="myApplications"
    :loading="loading"
    :record-columns="recordColumns"
    @form-submit="handleFormSubmit"
  >
    <template #form>
      <ApplyForm
        :assetOptions="assetOptions"
        @submit="handleFormSubmit"
      />
    </template>
    
    <template #column-status="{ row }">
      <StatusTag :status="row.status" />
    </template>
  </ApplyPage>
</template>

<script setup>
const recordColumns = [
  { prop: 'id', label: '申请编号', width: '100' },
  { type: 'custom', prop: 'status', label: '申请状态', width: '120' },
  { prop: 'approverName', label: '审批人', width: '120' }
]

const handleFormSubmit = async (formData) => {
  // 提交申请
}
</script>
```

---

## 组件命名规范

1. **通用组件**：使用 PascalCase，放在 `components/common/` 目录下
   - 例：`SearchForm.vue`, `DataTable.vue`

2. **业务组件**：使用 PascalCase，放在 `components/business/` 目录下
   - 例：`ManagementPage.vue`, `ApplyPage.vue`

3. **Props 命名**：使用 camelCase
   - 例：`formFields`, `showPagination`

4. **Events 命名**：使用 camelCase
   - 例：`formSubmit`, `pageChange`

5. **Slots 命名**：使用 kebab-case
   - 例：`column-status`, `form-footer`

---

## 最佳实践

1. **组件复用**：优先使用通用组件，避免重复代码
2. **Props 传递**：只传递必要的数据，避免过度传递
3. **事件处理**：使用事件向父组件传递操作结果
4. **插槽使用**：利用插槽实现内容自定义，提高组件灵活性
5. **样式隔离**：使用 scoped 样式，避免样式冲突

---

## 兼容性说明

- 支持 Vue 3.0+
- 支持 Element Plus 2.0+
- 支持现代浏览器（Chrome、Firefox、Safari、Edge）
- 支持响应式布局

---

## 性能优化建议

1. **懒加载**：使用 `defineAsyncComponent` 进行组件懒加载
2. **虚拟滚动**：大数据量表格使用虚拟滚动
3. **防抖节流**：搜索输入使用防抖，按钮点击使用节流
4. **缓存优化**：合理使用 `keep-alive` 缓存组件状态

# 资产申请审批流程

## 申请类型

资产系统支持五种申请类型，对应不同业务场景。

### RECEIVE - 领用

员工申请领用闲置或在库资产，用于日常工作使用。

### TRANSFER - 调拨

资产在不同部门或人员之间转移，变更资产归属。

### MAINTENANCE - 维修

资产出现故障或需要保养时提交维修申请。

### REPAIR - 修理

与维修类型类似，用于资产修理场景。

### DISPOSAL - 报废

资产达到使用年限或无法修复时提交报废申请，需要二级审批。

## 申请状态

所有申请状态存储于asset_application表的status字段，系统定义七种状态。

### pending

待审批。申请人提交申请后的初始状态，等待审批人处理。

### pending_leader

待领导审批。资产报废申请中，部门资产管理员初审通过后的状态，等待部门领导终审。

### leader_approved

领导已批准。部门领导审批通过后的状态，等待资产管理员最终确认。

### approved

已批准。申请最终通过，系统自动执行相应业务操作。

### rejected

已拒绝。申请被驳回，申请人可重新提交或撤销。

### in_progress

进行中。申请批准后的执行状态，如资产维修进行中。

### completed

已完成。业务流程全部执行完毕。

## 审批流程

### 一级审批流程

适用于领用、调拨、维修、修理四种申请类型。

流程步骤：

1. 申请人提交申请，状态为pending
2. 审批人（部门资产管理员）查看待审批列表
3. 审批人选择批准或拒绝
4. 批准后状态变为approved，系统执行相应业务操作
5. 拒绝后状态变为rejected

角色权限：

- ADMIN：可审批所有申请
- LEADER：可审批本部门申请
- MANAGER：可审批本部门申请
- USER：不可审批，仅可提交申请

### 二级审批流程

适用于资产报废申请（DISPOSAL类型）。

流程步骤：

1. 用户提交报废申请，状态为pending
2. 部门资产管理员初审，批准后状态变为pending_leader
3. 部门领导终审，批准后状态变为leader_approved
4. 资产管理员最终确认，状态变为approved
5. 系统自动将资产状态更新为scrapped

角色权限：

- ADMIN：可执行所有审批步骤
- LEADER：可执行领导审批步骤（pending_leader -> leader_approved）
- MANAGER：可执行初审和最终确认步骤（pending -> pending_leader，leader_approved -> approved）
- USER：仅可提交申请

### 驳回逻辑

审批人可随时拒绝申请，拒绝后状态变为rejected。

拒绝规则：

- pending状态可被拒绝，直接变为rejected
- pending_leader状态可被拒绝，直接变为rejected
- leader_approved状态可被拒绝，直接变为rejected
- rejected状态的申请可重新提交，重置为pending

### 终审申请创建

资产报废申请需要创建终审申请，用于提交给部门领导审批。

创建条件：

- 仅报废申请（DISPOSAL）需要创建终审申请
- 由资产管理员创建终审申请
- 创建后生成新的申请记录，关联原申请ID

## 状态流转图

### 一级审批流转

pending -> approved

pending -> rejected

### 二级审批流转（报废申请）

pending -> pending_leader -> leader_approved -> approved

pending -> rejected

pending_leader -> rejected

leader_approved -> rejected

## 申请数据结构

申请记录存储于asset_application表，关键字段包括：

- asset_id：关联资产ID
- application_type：申请类型
- applicant_id：申请人ID
- applicant_name：申请人姓名
- department_id：部门ID
- status：申请状态
- approver_id：审批人ID
- approver_name：审批人姓名
- approval_date：审批日期
- approval_remark：审批备注
- original_application_id：关联原申请ID（用于二级审批）

## 审批操作接口

- POST /api/asset-applications/{id}/approve：批准申请
- POST /api/asset-applications/{id}/reject：拒绝申请
- POST /api/asset-applications/create-final-approval：创建终审申请

## 数据权限过滤

申请列表查询时的数据过滤规则：

- ADMIN：查看所有申请
- LEADER：查看本部门申请
- MANAGER：查看本部门申请
- USER：仅查看自己提交的申请

状态查询接口支持按状态和部门筛选，便于审批人快速定位待处理申请。
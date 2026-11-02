# 企业资产智能管理平台 - 技术文档

## 1. 项目概述

### 1.1 项目名称
企业资产智能管理平台 (Asset AI Platform)

### 1.2 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 后端 | Java | 17 |
| 框架 | Spring Boot | 3.2.10 |
| ORM | Spring Data JPA | 3.2.x |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| 向量检索 | Redis Stack（RediSearch） | 7.4 |
| AI 框架 | Spring AI | 1.0.0-M4 |
| 大模型 | 阿里云百炼 DashScope（qwen-plus / text-embedding-v2） | - |
| 微服务 | Spring Cloud Alibaba | 2023.0.x |
| 服务发现 | Nacos | 3.2.x |
| 远程调用 | OpenFeign | 3.2.x |
| 前端 | Vue | 3.x |
| 构建工具 | Maven | 3.9+ |

### 1.3 项目结构

```
asset-ai-platform/
├── asset-auth/                  # 认证服务模块
│   └── src/main/java/com/enterprise/asset/auth/
│       ├── controller/          # 认证控制器
│       ├── service/             # 认证服务
│       ├── repository/          # 认证数据访问层
│       ├── entity/              # 用户/角色/权限实体
│       ├── security/            # Spring Security配置
│       └── config/              # 认证配置
├── asset-business/              # 业务服务模块
│   └── src/main/java/com/enterprise/asset/business/
│       ├── controller/          # 业务控制器
│       ├── service/             # 业务服务（含Redis缓存）
│       ├── repository/          # 业务数据访问层
│       ├── entity/              # 资产/申请/折旧等实体
│       ├── config/              # Redis配置类
│       └── client/              # Feign客户端
├── asset-ai/                    # AI 知识助手服务模块（端口 8083）
│   └── src/main/java/com/enterprise/asset/ai/
│       ├── controller/          # 会话/消息/SSE 接口
│       ├── service/             # Chat 流式对话、RAG 检索、知识库加载
│       ├── repository/          # 会话与消息数据访问层
│       ├── entity/              # AiConversation / AiMessage
│       ├── security/            # 主系统 JWT 解析
│       ├── config/              # OpenAI、Redis Vector Store、异步、异常处理
│       └── dto/                 # 会话/消息请求响应对象
│       resources/knowledge/     # 制度知识库 Markdown 文档
│       resources/sql/           # ai_conversation / ai_message 建表脚本
├── asset-common/                # 公共模块
│   └── src/main/java/com/enterprise/asset/common/
│       ├── dto/                 # 数据传输对象
│       ├── enums/               # 枚举类
│       ├── security/            # JWT工具类
│       └── util/                # 公共工具类
│   * 注意：common禁止存放@Entity实体，实体仅存在于各服务模块
├── frontend/                    # 前端Vue项目
│   ├── src/
│   │   ├── components/          # 公共组件
│   │   ├── config/              # 配置文件
│   │   ├── router/              # 路由配置
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   └── vite.config.js           # Vite配置
└── pom.xml                      # Maven父工程依赖管理
```

---

## 2. 核心业务功能

### 2.1 功能模块

| 模块 | 说明 | 状态 |
|------|------|------|
| 资产管理 | 资产的增删改查、状态管理 | ✅ |
| 资产领用 | 资产领用申请与审批 | ✅ |
| 资产转移 | 资产部门/人员转移 | ✅ |
| 资产维修 | 维修申请与进度追踪 | ✅ |
| 资产报废 | 二级审批报废流程 | ✅ |
| 资产盘点 | 盘点计划与执行 | ✅ |
| 折旧管理 | 自动折旧计算 | ✅ |
| 采购管理 | 采购申请与订单 | ✅ |
| 报表统计 | 资产统计报表 | ✅ |
| AI 知识助手 | 制度知识库 RAG 问答（SSE 流式） | ✅ |

### 2.2 核心业务流程

#### 2.2.1 资产报废二级审批流程

```
用户提交报废申请 → pending → 资产管理员初审 → pending_leader → 领导审批 → approved → 资产状态更新为scrapped
                                                    ↓
                                              rejected（拒绝）
```

**状态流转说明：**

| 状态 | 说明 | 操作权限 |
|------|------|----------|
| `pending` | 待审批 | 资产管理员可初审 |
| `pending_leader` | 待领导审批 | 领导可审批 |
| `approved` | 已批准 | 系统自动更新资产状态 |
| `rejected` | 已拒绝 | 可重新提交或撤销 |

---

## 3. 数据库设计

### 3.1 核心表结构

#### 3.1.1 asset（资产表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 资产ID |
| asset_no | VARCHAR(255) | UNIQUE, NOT NULL | 资产编号 |
| asset_name | VARCHAR(255) | NOT NULL | 资产名称 |
| category_id | BIGINT | NULL | 分类ID |
| model | VARCHAR(255) | NULL | 型号规格 |
| purchase_price | DECIMAL(38,2) | NULL | 购入价格 |
| net_value | DECIMAL(38,2) | NULL | 净值 |
| original_value | DECIMAL(38,2) | NOT NULL | 原值 |
| supplier_id | BIGINT | NULL | 供应商ID |
| purchase_date | DATE | NULL | 购入日期 |
| useful_life | INT | NOT NULL | 使用年限（月） |
| depreciation_method | VARCHAR(255) | NULL | 折旧方法 |
| status | VARCHAR(255) | NULL | 资产状态 |
| use_status | VARCHAR(255) | NULL | 使用状态 |
| dept_id | BIGINT | NULL | 所属部门ID |
| user_id | BIGINT | NULL | 使用人ID |
| borrow_status | VARCHAR(255) | NULL | 借出状态 |
| borrower_id | BIGINT | NULL | 借用人ID |
| borrow_time | DATETIME(6) | NULL | 借出时间 |
| expected_return_time | DATETIME(6) | NULL | 预计归还时间 |

#### 3.1.2 asset_application（资产业务申请表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 申请ID |
| asset_id | BIGINT | NULL | 关联资产ID |
| application_type | VARCHAR(255) | NOT NULL | 申请类型：RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL |
| applicant_id | BIGINT | NOT NULL | 申请人ID |
| applicant_name | VARCHAR(255) | NOT NULL | 申请人姓名 |
| department_id | BIGINT | NULL | 部门ID |
| status | VARCHAR(255) | NULL | 申请状态 |
| approver_id | BIGINT | NULL | 审批人ID |
| approval_date | DATETIME | NULL | 审批日期 |
| approval_remark | TEXT | NULL | 审批备注 |
| original_application_id | BIGINT | NULL | 关联原申请ID（用于二级审批） |

#### 3.1.3 user（用户表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(255) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt加密） |
| real_name | VARCHAR(255) | NULL | 真实姓名 |
| dept_id | BIGINT | NULL | 所属部门ID |
| role | VARCHAR(255) | NULL | 角色：ADMIN/LEADER/MANAGER/USER（大写） |
| status | INT | NOT NULL | 状态：1-启用，0-禁用 |

#### 3.1.4 ai_conversation（AI 会话表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 会话ID |
| user_id | BIGINT | NOT NULL | 所属用户ID |
| title | VARCHAR(200) | NOT NULL | 会话标题（取自首条提问） |
| create_time | DATETIME(6) | NOT NULL | 创建时间 |
| update_time | DATETIME(6) | NOT NULL | 更新时间 |

#### 3.1.5 ai_message（AI 消息表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 消息ID |
| conversation_id | BIGINT | NOT NULL | 会话ID |
| role | VARCHAR(20) | NOT NULL | user / assistant |
| content | TEXT | NOT NULL | 消息内容 |
| create_time | DATETIME(6) | NOT NULL | 创建时间 |

### 3.2 索引设计

| 表名 | 索引名称 | 字段 | 类型 |
|------|----------|------|------|
| asset | PRIMARY | id | 主键 |
| asset | asset_no | asset_no | 唯一索引 |
| asset | idx_category_id | category_id | 普通索引 |
| asset | idx_status | status | 普通索引 |
| asset | idx_user_id | user_id | 普通索引 |
| asset | idx_dept_id | dept_id | 普通索引 |
| asset | idx_category_status | category_id, status | 联合索引 |
| asset_application | idx_application_status_dept | status, department_id | 联合索引 |

---

## 4. API接口设计

### 4.1 资产管理接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/assets` | GET | 获取资产列表（支持分页、Redis缓存） |
| `/api/assets/{id}` | GET | 获取单个资产详情 |
| `/api/assets` | POST | 新增资产 |
| `/api/assets/{id}` | PUT | 更新资产信息 |
| `/api/assets/{id}` | DELETE | 删除资产 |
| `/api/assets/status/{status}` | GET | 按状态查询资产 |
| `/api/assets/department/{deptId}` | GET | 按部门查询资产 |

**资产列表分页接口参数说明：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 0 | 页码（从0开始） |
| `size` | int | 10 | 每页条数 |
| `status` | string | - | 资产状态筛选（可选） |
| `sortBy` | string | id | 排序字段 |
| `sortDir` | string | desc | 排序方向（asc/desc） |

### 4.2 资产业务申请接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/asset-applications` | GET | 获取申请列表 |
| `/api/asset-applications/{id}` | GET | 获取申请详情 |
| `/api/asset-applications` | POST | 创建申请 |
| `/api/asset-applications/{id}/approve` | POST | 批准申请 |
| `/api/asset-applications/{id}/reject` | POST | 拒绝申请 |
| `/api/asset-applications/status/pending_leader` | GET | 获取待领导审批列表 |
| `/api/asset-applications/status/pending_leader/department/{deptId}` | GET | 获取部门待领导审批列表 |

### 4.3 用户认证接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/me` | GET | 获取当前用户信息 |

### 4.4 报表接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/reports/asset-status` | GET | 资产状态分布报表 |
| `/api/reports/department-asset` | GET | 部门资产统计报表 |
| `/api/reports/depreciation-summary` | GET | 折旧汇总报表 |

### 4.5 AI 知识助手接口

| API路径 | HTTP方法 | 功能描述 |
|---------|----------|----------|
| `/api/ai/chat/conversations` | GET | 当前用户会话列表 |
| `/api/ai/chat/conversations` | POST | 创建会话 |
| `/api/ai/chat/conversations/{id}` | GET | 会话详情（含归属校验） |
| `/api/ai/chat/conversations/{id}` | DELETE | 删除会话及消息 |
| `/api/ai/chat/conversations/{id}/messages` | GET | 分页获取消息 |
| `/api/ai/chat/messages` | POST | 同步发送消息 |
| `/api/ai/chat/messages/stream` | POST | SSE 流式发送消息 |
| `/api/ai/chat/messages/{id}` | DELETE | 删除单条消息 |
| `/api/ai/chat/messages/batch-delete` | POST | 批量删除消息 |

---

## 5. 安全与权限

### 5.1 角色定义

| 角色 | 权限描述 |
|------|----------|
| `ADMIN` | 系统管理员，拥有所有权限 |
| `LEADER` | 部门领导，可审批报废申请 |
| `MANAGER` | 资产管理员，可管理资产、处理申请 |
| `USER` | 普通用户，可提交申请、查看自己的资产 |

### 5.2 权限控制策略

- **基于角色的访问控制（RBAC）**
- **JWT Token认证**
- **密码使用BCrypt加密存储**
- **API请求需要携带有效Token**

---

## 6. 核心业务逻辑

### 6.1 资产报废二级审批

```java
// 审批逻辑（简化版，角色代码与数据库一致为大写）
if ("DISPOSAL".equals(applicationType)) {
    // 资产管理员初审：pending -> pending_leader
    if (isManager && "pending".equals(status)) {
        application.setStatus("pending_leader");
    }
    // 领导终审：pending_leader -> approved，同步更新资产状态为scrapped
    else if ((isAdmin || isLeader) && "pending_leader".equals(status)) {
        application.setStatus("approved");
        asset.setStatus("scrapped");
    }
    // 任意环节被驳回 -> rejected
}
```

### 6.2 折旧计算策略

系统支持多种折旧方法：

| 方法 | 类名 | 说明 |
|------|------|------|
| 直线法 | `StraightLineDepreciationCalculator` | 平均年限折旧 |
| 双倍余额递减法 | `DoubleDecliningBalanceDepreciationCalculator` | 加速折旧 |
| 工作量法 | `WorkUnitDepreciationCalculator` | 按工作量折旧 |

### 6.3 Redis缓存策略

资产列表查询接口引入Redis缓存，提升查询性能：

**缓存机制：**
- **缓存时间**：5分钟自动过期
- **缓存键设计**：`asset_list:user_{userId}:admin_{true/false}:leader_{true/false}:dept_{deptId}:page_{page}:size_{size}:status_{status}:sort_{sortBy}_{sortDir}`
- **缓存失效**：资产增删改操作时自动清除所有资产列表缓存
- **降级处理**：Redis连接失败时自动降级为数据库查询，不影响业务

**性能对比：**

| 查询场景 | 数据库查询 | Redis缓存 | 性能提升 |
|----------|-----------|----------|----------|
| 首次查询 | ~450ms | - | - |
| 二次查询 | - | ~45ms | ~90% |

**核心代码逻辑：**

```java
public Page<Asset> getAssetsWithPagination(int page, int size, String status, String sortBy, String sortDir) {
    String cacheKey = buildCacheKey(page, size, status, sortBy, sortDir, userId, isAdmin, isLeaderOrManager, deptId);
    
    Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
    if (cachedObj != null) {
        return objectMapper.readValue(objectMapper.writeValueAsString(cachedObj), 
            new TypeReference<Page<Asset>>() {});
    }
    
    Page<Asset> result = assetRepository.findAll(pageable);
    
    redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
    
    return result;
}
```

### 6.4 AI 知识助手 RAG 策略

**知识库加载（启动时）：**
- 扫描 `classpath:knowledge/**/*.md`
- 按文件内容 MD5 去重，未变更文件跳过（Redis key `ai:rag:processed_files`）
- `TokenTextSplitter(800, 200)` 分块 → `text-embedding-v2` → Redis Vector Store
- 文档 ID 使用 SHA-256 确定性生成，重复加载不会产生重复向量

**检索与回答：**
- 用户提问 → `text-embedding-v2` 向量化 → Redis 向量检索
- `top-k=3`、`similarity-threshold=0.30`，应用层按 `tenant` 过滤
- 检索结果拼入 `SystemMessage`，再由 qwen-plus 生成回答
- SSE 事件：`event:message`（增量内容）/ `event:error`（错误信息）
- 无命中时降级为通用回答，不阻塞对话

**会话与缓存：**
- 会话与消息持久化到 `ai_conversation` / `ai_message`
- 历史消息缓存 key：`ai:chat:history:{conversationId}`，TTL 24 小时
- Redis 写入失败自动降级为数据库查询，不影响对话

**前置条件：**
- Redis 必须使用 `redis/redis-stack-server:7.4.0-v3`（含 RediSearch）
- 必须配置环境变量 `ALIYUN_API_KEY`（阿里云百炼）

---

## 7. 部署与运行

### 7.1 环境要求

- JDK 17+
- MySQL 8.0+
- Redis Stack 7.4+（AI 向量检索依赖 RediSearch）
- Maven 3.9+
- Nacos 3.2.x（微服务模式）
- 阿里云百炼 API Key（环境变量 `ALIYUN_API_KEY`）

### 7.2 启动方式

```bash
# 1. 启动基础设施（Redis Stack + Nacos）
docker compose up -d

# 2. 启动认证服务
mvn -pl asset-auth spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 启动业务服务
mvn -pl asset-business spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 启动 AI 知识助手（需先配置 ALIYUN_API_KEY）
export ALIYUN_API_KEY=sk-your-api-key
mvn -pl asset-ai spring-boot:run

# 5. 启动 API 网关
mvn -pl asset-gateway spring-boot:run

# 6. 前端启动（开发模式）
cd frontend
npm install
npm run dev
```

### 7.3 配置说明

业务服务配置文件：`asset-business/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/asset_management?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public
```

AI 助手配置文件：`asset-ai/src/main/resources/application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: asset-ai
  ai:
    openai:
      api-key: ${ALIYUN_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-plus
      embedding:
        options:
          model: text-embedding-v2
    vectorstore:
      redis:
        uri: redis://localhost:6379
        index: asset_knowledge_index
        initialize-schema: true
        prefix: "asset:rag:vector:"

rag:
  retrieval:
    top-k: 3
    similarity-threshold: 0.30
```

---

## 8. 代码质量保证

### 8.1 测试覆盖

| 测试类型 | 路径 | 说明 |
|----------|------|------|
| 单元测试 | `src/test/java/...` | 业务逻辑单元测试 |
| 集成测试 | `src/test/java/...` | 服务层集成测试 |

### 8.2 测试示例

```java
// 折旧计算器测试
@Test
void testStraightLineDepreciation() {
    BigDecimal assetValue = new BigDecimal("10000");
    int usefulLife = 60; // 5年 = 60个月
    
    BigDecimal depreciation = calculator.calculate(assetValue, usefulLife, 1);
    assertEquals(new BigDecimal("166.67"), depreciation.setScale(2, RoundingMode.HALF_UP));
}
```

---

## 9. 维护与扩展

### 9.1 日志管理

系统日志记录到 `sys_log` 表：

| 字段 | 说明 |
|------|------|
| user_id | 操作用户ID |
| operation | 操作描述 |
| log_type | 日志类型 |
| status | 操作状态 |

### 9.2 性能优化建议

1. **索引优化**：根据查询频率合理创建索引，已为asset表创建category_id、status、user_id、dept_id索引及联合索引idx_category_status
2. **查询优化**：避免SELECT *，使用覆盖索引
3. **分页优化**：使用JPA Pageable分页，避免大数据量一次性加载
4. **Redis缓存**：资产列表查询引入Redis缓存，缓存时间5分钟，资产增删改自动失效，性能提升约90%
5. **缓存键设计**：包含用户ID、角色、部门ID、分页参数、排序参数，确保多维度数据隔离

---

## 附录：ER图关系

```
user 1 ─── * asset_application (申请人)
user 1 ─── * asset (使用人)
department 1 ─── * asset (所属部门)
department 1 ─── * user (部门用户)
asset_category 1 ─── * asset (资产分类)
supplier 1 ─── * asset (供应商)
asset 1 ─── * asset_application (关联申请)
asset 1 ─── * asset_depreciation (折旧记录)
asset_inventory 1 ─── * asset_inventory_detail (盘点明细)
```

# 企业资产智能管理平台

[![GitHub Stars](https://img.shields.io/github/stars/ilovederrickrose5/asset-ai-platform.svg?style=flat)](https://github.com/ilovederrickrose5/asset-ai-platform/stargazers)
[![GitHub License](https://img.shields.io/github/license/ilovederrickrose5/asset-ai-platform.svg?style=flat)](https://github.com/ilovederrickrose5/asset-ai-platform/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/technologies/downloads/#java17)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.10-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.13-blue.svg)](https://element-plus.org/)

企业级资产全生命周期管理平台，基于 **Spring Cloud Alibaba 微服务架构 + Vue 3** 构建，覆盖资产入库、领用、转移、维修、报废、盘点、折旧与采购管理，并内置基于 **RAG** 的智能知识助手。

## 🖼️ 系统界面预览

| 登录页 | 系统概览 |
|:------:|:--------:|
| ![登录页](images/login.png) | ![系统概览](images/dashboard.png) |

| 资产列表 | 资产领用申请 |
|:--------:|:------------:|
| ![资产列表](images/asset-list.png) | ![资产领用申请](images/asset-apply.png) |

| 资产审批 | 资产转移申请 |
|:--------:|:------------:|
| ![资产审批](images/asset-approval.png) | ![资产转移申请](images/asset-transfer.png) |

| 资产报废申请 |
|:------------:|
| ![资产报废申请](images/asset-disposal.png) |

## 📋 目录

- [1. 项目概述](#1-项目概述)
  - [1.1 技术栈](#11-技术栈)
- [2. 微服务架构](#2-微服务架构)
  - [2.1 模块划分](#21-模块划分)
  - [2.2 服务职责](#22-服务职责)
  - [2.3 服务间调用](#23-服务间调用)
- [3. 系统功能模块](#3-系统功能模块)
  - [3.1 功能模块总览](#31-功能模块总览)
  - [3.2 角色权限体系](#32-角色权限体系)
- [4. 数据库设计](#4-数据库设计)
- [5. API接口文档](#5-api接口文档)
- [6. 核心业务流程](#6-核心业务流程)
- [7. 安全机制](#7-安全机制)
- [8. 配置说明](#8-配置说明)
- [9. 快速开始](#9-快速开始)
- [10. 项目结构](#10-项目结构)
- [11. 系统特性](#11-系统特性)
- [12. 版本信息](#12-版本信息)
- [13. 许可证](#13-许可证)

---

## 1. 项目概述

企业资产智能管理平台是一个基于 **Spring Cloud Alibaba 微服务架构** 的企业级资产全生命周期管理平台，实现了对固定资产的采购入库、日常管理、折旧计算、盘点管理、维修报废等全流程管理，并集成基于 RAG 的 AI 知识助手，支持制度问答与流式对话。

系统已从单体应用拆分为微服务架构，包含认证服务、业务服务、AI 知识助手与 API 网关四个核心模块，通过 Nacos 实现服务注册与发现，使用 OpenFeign 进行跨服务调用。

### 1.1 技术栈

**后端技术栈：**

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.10 |
| 微服务 | Spring Cloud | 2023.0.1 |
| 服务注册 | Spring Cloud Alibaba Nacos | 2023.0.1.0 |
| API 网关 | Spring Cloud Gateway | 2023.0.1 |
| 远程调用 | OpenFeign | 3.2.10 |
| 熔断降级 | Sentinel | 2023.0.1.0 |
| 持久层 | Spring Data JPA | 3.2.10 |
| AI 框架 | Spring AI | 1.0.0-M4 |
| 大模型 | 阿里云百炼 DashScope（qwen-plus） | - |
| 向量检索 | Redis Stack（RediSearch） | 7.4 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| 安全 | Spring Security + JWT | - |
| 构建工具 | Maven | 3.9+ |
| Java版本 | JDK | 17 |

**前端技术栈：**

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 3.5.x |
| UI组件库 | Element Plus | 2.13.x |
| 状态管理 | Pinia | 3.0.x |
| 路由 | Vue Router | 4.x |
| HTTP客户端 | Axios | 1.x |
| 图表 | ECharts | 5.x |
| 构建工具 | Vite | 7.x |

---

## 2. 微服务架构

### 2.1 模块划分

```
asset-ai-platform/
├── asset-gateway/      # API网关（端口：9000）统一入口、路由转发、跨域处理
├── asset-auth/          # 认证服务（端口：8081）
│   ├── 用户管理
│   ├── 角色管理
│   ├── 权限管理
│   ├── 部门管理
│   ├── 系统日志
│   └── JWT认证
├── asset-business/      # 业务服务（端口：8082）
│   ├── 资产管理
│   ├── 资产折旧
│   ├── 资产盘点
│   ├── 资产业务申请
│   ├── 采购管理
│   ├── 报表统计
│   └── 仪表盘
├── asset-ai/            # AI 知识助手服务（端口：8083）
│   ├── 制度知识库 RAG 问答（SSE 流式输出）
│   ├── 会话与消息管理
│   └── 向量检索（Redis Stack / RediSearch）
├── asset-common/        # 公共模块
│   ├── DTO数据传输对象
│   ├── 枚举类
│   ├── JWT工具类
│   └── 统一返回工具
└── frontend/            # 前端服务（端口：5173）
```

### 2.2 服务职责

| 服务 | 职责 | 端口 |
|------|------|------|
| **asset-gateway** | API 网关,统一路由入口、跨域处理、对外隔离 | 9000 |
| **asset-auth** | 用户认证、角色权限、部门管理、系统日志 | 8081 |
| **asset-business** | 资产管理、折旧计算、盘点管理、采购管理、报表统计 | 8082 |
| **asset-ai** | 资产管理制度知识库 RAG 问答、SSE 流式输出、会话管理 | 8083 |
| **Nacos** | 服务注册与发现 | 8848 |
| **Sentinel Dashboard** | 熔断降级规则可视化(可选) | 8080 |
| **frontend** | 前端页面展示与交互 | 5173 |

### 2.3 服务间调用

```
frontend ──→ asset-gateway (9000) ──→ asset-auth (认证、用户、部门)
                  │
                  ├──→ asset-business (资产、折旧、盘点、采购、报表)
                  │             │
                  │             └──AuthFeignClient──→ asset-auth (跨服务调用,失败走 Sentinel 降级)
                  │
                  └──→ asset-ai (知识库问答、会话管理)
                                │
                                └──Redis Stack──→ 制度文档向量索引
                                └──DashScope────→ qwen-plus / text-embedding-v2
```

---

## 3. 系统功能模块

### 3.1 功能模块总览

```
├── 基础数据管理
│   ├── 部门管理
│   ├── 资产分类管理
│   ├── 员工信息管理
│   └── 供应商信息管理
│
├── 资产管理
│   ├── 资产登记
│   ├── 资产查询
│   ├── 资产编辑
│   └── 资产删除
│
├── 资产折旧
│   ├── 单资产折旧计算
│   ├── 批量折旧计算
│   ├── 折旧方法支持（直线法、双倍余额递减法、工作量法）
│   └── 折旧记录查询
│
├── 资产盘点
│   ├── 创建盘点计划
│   ├── 分配盘点任务
│   ├── 执行盘点
│   └── 盘点结果统计
│
├── 资产业务流程
│   ├── 资产领用申请
│   ├── 资产转移申请
│   ├── 资产维修申请
│   └── 资产报废申请（二级审批）
│
├── 采购管理
│   ├── 采购申请
│   ├── 采购审批
│   └── 采购订单
│
├── 报表统计
│   ├── 资产统计报表
│   ├── 部门资产分布
│   └── 资产状态分布
│
├── AI 知识助手
│   ├── 固定资产管理制度问答（RAG）
│   ├── SSE 流式回答
│   ├── 多会话管理
│   └── 消息删除与批量管理
│
└── 系统管理
    ├── 角色管理
    ├── 用户管理
    └── 操作日志
```

### 3.2 角色权限体系

系统采用基于角色的访问控制（RBAC），包含四种角色：

| 角色 | 角色代码 | 权限说明 |
|------|---------|---------|
| 系统管理员 | ADMIN | 拥有系统所有权限 |
| 部门领导 | LEADER | 查看统计报表、审批报废申请 |
| 部门资产管理员 | MANAGER | 管理本部门资产 |
| 普通员工 | USER | 发起各类资产申请 |

---

## 4. 数据库设计

### 4.1 数据库配置

```properties
# 数据库连接信息
spring.datasource.url=jdbc:mysql://localhost:3306/asset-ai
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
```

### 4.2 核心数据表

#### 4.2.1 资产表（asset）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_no | VARCHAR | 资产编号（唯一） |
| asset_name | VARCHAR | 资产名称 |
| category_id | BIGINT | 资产分类ID |
| model | VARCHAR | 规格型号 |
| purchase_price | DECIMAL | 购入价格 |
| original_value | DECIMAL | 原值 |
| net_value | DECIMAL | 当前净值 |
| supplier_id | BIGINT | 供应商ID |
| purchase_date | DATE | 购入日期 |
| useful_life | INT | 使用年限（月） |
| depreciation_method | VARCHAR | 折旧方法 |
| status | VARCHAR | 资产状态 |
| use_status | VARCHAR | 使用状态 |
| dept_id | BIGINT | 所属部门ID |
| user_id | BIGINT | 使用人ID |

#### 4.2.2 用户表（user）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| username | VARCHAR | 用户名（唯一） |
| password | VARCHAR | 密码 |
| real_name | VARCHAR | 真实姓名 |
| dept_id | BIGINT | 部门ID |
| role | VARCHAR | 角色代码 |
| status | INT | 账号状态 |

#### 4.2.3 部门表（department）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| dept_name | VARCHAR | 部门名称 |
| dept_code | VARCHAR | 部门编码 |
| parent_id | BIGINT | 上级部门ID |

#### 4.2.4 资产业务申请表（asset_application）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_id | BIGINT | 资产ID |
| application_type | VARCHAR | 申请类型（领用/转移/维修/报废） |
| applicant_id | BIGINT | 申请人ID |
| status | VARCHAR | 状态 |
| approver_id | BIGINT | 审批人ID |
| approval_remark | TEXT | 审批备注 |

#### 4.2.5 资产折旧记录表（asset_depreciation）

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT | 主键ID |
| asset_id | BIGINT | 资产ID |
| depreciation_method | VARCHAR | 折旧方法 |
| depreciation_amount | DECIMAL | 本期折旧额 |
| accumulated_depreciation | DECIMAL | 累计折旧 |
| net_value | DECIMAL | 折旧后净值 |

---

## 5. API接口文档

### 5.1 认证接口（asset-auth）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/me` | GET | 获取当前用户信息 |
| `/api/auth/validate-token` | POST | 验证Token有效性 |

### 5.2 用户管理接口（asset-auth）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/users` | GET | 获取用户列表 |
| `/api/users/{id}` | GET | 获取用户详情 |
| `/api/users` | POST | 创建用户 |
| `/api/users/{id}` | PUT | 更新用户 |
| `/api/users/{id}` | DELETE | 删除用户 |

### 5.3 部门管理接口（asset-auth）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/departments` | GET | 获取部门列表 |
| `/api/departments/{id}` | GET | 获取部门详情 |
| `/api/departments` | POST | 创建部门 |
| `/api/departments/{id}` | PUT | 更新部门 |
| `/api/departments/{id}` | DELETE | 删除部门 |

### 5.4 资产管理接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/assets` | GET | 获取资产列表（支持分页、Redis缓存） |
| `/api/assets/{id}` | GET | 获取资产详情 |
| `/api/assets` | POST | 创建资产 |
| `/api/assets/{id}` | PUT | 更新资产 |
| `/api/assets/{id}` | DELETE | 删除资产 |

**资产列表分页参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 0 | 页码（从0开始） |
| `size` | int | 10 | 每页条数 |
| `status` | string | - | 资产状态筛选（可选） |
| `sortBy` | string | id | 排序字段 |
| `sortDir` | string | desc | 排序方向（asc/desc） |

### 5.5 资产折旧接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/depreciation/calculate` | POST | 计算折旧 |
| `/api/depreciation/batch-calculate` | POST | 批量计算折旧 |
| `/api/depreciation/records` | GET | 获取折旧记录 |
| `/api/depreciation/summary` | GET | 获取折旧汇总 |

### 5.6 资产盘点接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/asset-inventory/plans` | GET | 获取盘点计划列表 |
| `/api/asset-inventory/plans` | POST | 创建盘点计划 |
| `/api/asset-inventory/plans/{id}/assign` | POST | 分配盘点任务 |
| `/api/asset-inventory/plans/{id}/start` | POST | 开始盘点 |
| `/api/asset-inventory/plans/{id}/complete` | POST | 完成盘点 |

### 5.7 资产业务申请接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/asset-applications` | GET | 获取申请列表 |
| `/api/asset-applications` | POST | 创建申请 |
| `/api/asset-applications/{id}/approve` | POST | 审批通过 |
| `/api/asset-applications/{id}/reject` | POST | 审批拒绝 |

### 5.8 采购管理接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/purchase-requests` | GET | 获取采购申请列表 |
| `/api/purchase-requests` | POST | 创建采购申请 |
| `/api/purchase-requests/{id}/approve` | POST | 审批采购申请 |
| `/api/purchase-orders` | GET | 获取采购订单列表 |
| `/api/purchase-orders` | POST | 创建采购订单 |

### 5.9 报表统计接口（asset-business）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/dashboard/stats` | GET | 获取仪表盘统计 |
| `/api/dashboard/recent-operations` | GET | 获取最近操作 |
| `/api/reports/assets` | GET | 资产统计报表 |
| `/api/reports/departments` | GET | 部门资产报表 |

### 5.10 AI 知识助手接口（asset-ai）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/ai/chat/conversations` | GET | 获取当前用户会话列表 |
| `/api/ai/chat/conversations` | POST | 创建新会话 |
| `/api/ai/chat/conversations/{id}` | GET | 获取会话详情 |
| `/api/ai/chat/conversations/{id}` | DELETE | 删除会话及其消息 |
| `/api/ai/chat/conversations/{id}/messages` | GET | 分页获取会话消息 |
| `/api/ai/chat/messages` | POST | 发送消息（同步返回完整回答） |
| `/api/ai/chat/messages/stream` | POST | 发送消息（SSE 流式返回） |
| `/api/ai/chat/messages/{id}` | DELETE | 删除单条消息 |
| `/api/ai/chat/messages/batch-delete` | POST | 批量删除消息 |

> AI 接口从主系统 JWT 中解析 `uid` / `roles` / `deptId`，无需额外登录；经网关访问时路径保持 `/api/ai/chat/**`。
> 知识库文档位于 `asset-ai/src/main/resources/knowledge/**/*.md`，服务启动时自动向量化并写入 Redis Stack 索引。

---

## 6. 核心业务流程

### 6.1 资产折旧流程

系统支持三种折旧方法：

1. **直线法**：每期折旧额 =（原值 - 残值）/ 使用年限
2. **双倍余额递减法**：年折旧率 = 2 / 使用年限 × 100%
3. **工作量法**：单位工作量折旧 =（原值 - 残值）/ 总工作量

### 6.2 资产盘点流程

```
创建盘点计划 → 分配盘点任务 → 开始盘点 → 执行盘点 → 完成盘点
     │              │              │            │            │
     ▼              ▼              ▼            ▼            ▼
   生成明细      记录日志      记录日志     更新明细     统计结果
```

### 6.3 资产报废流程（二级审批）

```
部门资产管理员申请 → 部门领导审批 → 资产报废
                        ↓
                   审批拒绝（可重新申请）
```

### 6.4 采购流程

```
采购申请 → 部门领导审批 → 生成采购订单 → 采购入库
```

---

## 7. 安全机制

### 7.1 JWT认证

```
用户登录 → 生成JWT Token → 前端存储Token → 后续请求携带Token → 服务端验证Token
```

### 7.2 跨服务认证

业务服务通过 Feign 调用认证服务的 `/api/auth/validate-token` 接口验证 Token 有效性。

### 7.3 权限控制

| 接口前缀 | 允许访问角色 |
|---------|-------------|
| `/api/admin/**` | ADMIN |
| `/api/leader/**` | LEADER, ADMIN |
| `/api/manager/**` | MANAGER, LEADER, ADMIN |
| `/api/assets/**` | USER, MANAGER, LEADER, ADMIN |

---

## 8. 配置说明

### 8.1 Nacos配置

所有服务需注册到 Nacos：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
```

### 8.2 服务配置

**asset-auth（认证服务）：**

```yaml
server:
  port: 8081
spring:
  application:
    name: asset-auth
```

**asset-business（业务服务）：**

```yaml
server:
  port: 8082
spring:
  application:
    name: asset-business
```

**asset-ai（AI 知识助手）：**

```yaml
server:
  port: 8083
spring:
  application:
    name: asset-ai
  ai:
    openai:
      api-key: ${ALIYUN_API_KEY}          # 阿里云百炼 API Key
      base-url: https://dashscope.aliyuncs.com/compatible-mode
    vectorstore:
      redis:
        index: asset_knowledge_index
        prefix: "asset:rag:vector:"

rag:
  retrieval:
    top-k: 3
    similarity-threshold: 0.30
```

### 8.3 前端代理配置

Vite 将 `/api` 统一代理到 API 网关（9000），与生产环境入口保持一致，由网关按路径分发到各服务：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:9000', // asset-gateway
    changeOrigin: true,
    configure: (proxy) => {
      // SSE 流式响应需要关闭压缩缓冲
      proxy.on('proxyReq', (req) => req.setHeader('Accept-Encoding', 'identity'))
    }
  }
}
```

> 开发时需同时启动 `asset-gateway`；网关依赖 Nacos 中的服务注册信息完成路由。

---

## 9. 快速开始

### 9.1 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20.19+ 或 22.12+（Vite 7 与 marked 18 的最低要求）
- MySQL 8.0+（本机安装即可）
- Docker Desktop（可选，用于一键启动 Redis 与 Nacos）
- Redis Stack 7.4+（AI 向量检索依赖 RediSearch，普通 Redis 镜像不支持）
- Nacos 3.2+（如不使用 Docker，需自行安装）
- 阿里云百炼 API Key（`ALIYUN_API_KEY`，AI 助手必需）
- Sentinel Dashboard 1.8.x（可选，熔断规则可视化）

### 9.2 数据库初始化

```sql
CREATE DATABASE `asset-ai` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库表结构由 JPA 在服务启动时自动创建/更新。默认连接配置：`localhost:3306`，用户名 `root`，密码通过环境变量 `DB_PASSWORD` 提供。

### 9.3 启动步骤

**0. 配置环境变量：**

```bash
cp .env.example .env
```

填写 `.env` 中的 `DB_PASSWORD`、`JWT_SECRET`（≥32 字符）、`ALIYUN_API_KEY`、`NACOS_AUTH_TOKEN`、`NACOS_AUTH_IDENTITY_VALUE`。`.env` 已被 `.gitignore` 忽略，不会提交；`start-all.bat` 会自动加载它。

**1. 启动基础设施（Redis + Nacos）：**

```bash
# 在项目根目录执行，MySQL 复用本机 3306
docker compose up -d
```

> AI 向量检索要求 Redis Stack（`redis/redis-stack-server:7.4.0-v3`），`docker-compose.yml` 已按此镜像配置。
> 若本机已安装 Redis Stack 与 Nacos，也可手动启动：`redis-stack-server`、`startup.cmd -m standalone`。

**2. 编译项目：**

```bash
mvn clean install -DskipTests
```

**3. 启动认证服务（8081）：**

```bash
mvn -pl asset-auth spring-boot:run
```

**4. 启动业务服务（8082）：**

```bash
mvn -pl asset-business spring-boot:run
```

**5. 启动 AI 知识助手（8083）：**

```bash
# Windows PowerShell
$env:ALIYUN_API_KEY="sk-你的百炼Key"
# macOS / Linux
export ALIYUN_API_KEY="sk-你的百炼Key"

mvn -pl asset-ai spring-boot:run
```

> 首次启动会自动扫描 `asset-ai/src/main/resources/knowledge/` 下的 Markdown 文档，调用 text-embedding-v2 生成向量并写入 Redis；后续启动按文件 MD5 跳过未变更文档。

**6. 启动前端：**

```bash
cd frontend
npm install
npm run dev
```

**7.（可选）启动 API 网关（9000）：**

```bash
mvn -pl asset-gateway spring-boot:run
```

### 9.4 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| API 网关 | http://localhost:9000 |
| 认证服务 | http://localhost:8081 |
| 业务服务 | http://localhost:8082 |
| AI 知识助手 | http://localhost:8083 |
| Nacos 控制台 | http://localhost:8848/nacos |

### 9.5 演示账号

初始化数据包含以下账号，密码由初始化脚本设置，首次登录后请尽快修改：

| 用户名 | 角色 | 说明 |
|--------|------|------|
| `admin` | 系统管理员 | 全部权限，包含用户、部门、角色管理 |
| `admin_yy` | 资产管理员 | 运营部资产管理员，可管理资产、初审报废 |
| `leader_yy` | 部门领导 | 运营部领导，可终审报废申请 |
| `user_yy_1` | 普通员工 | 运营部员工，可提交领用、转移、维修、报废申请 |

---

## 10. 项目结构

### 10.1 后端模块结构

```
asset-auth/
├── controller/       # REST API控制器
├── service/          # 业务服务层
├── repository/       # 数据访问层
├── entity/           # JPA实体类
├── security/         # 安全认证模块
├── config/           # 配置类
└── AuthApplication.java

asset-business/
├── controller/       # REST API控制器
├── service/          # 业务服务层
├── repository/       # 数据访问层
├── entity/           # JPA实体类
├── client/           # Feign客户端
├── security/         # 安全认证模块
├── config/           # 配置类
└── BusinessApplication.java

asset-ai/
├── controller/       # AI 会话、消息、SSE 接口
├── service/          # RAG 检索、Chat 流式处理、知识库加载
├── repository/       # 会话与消息数据访问层
├── entity/           # AiConversation / AiMessage
├── security/         # 主系统 JWT 解析
├── config/           # OpenAI、Redis Vector Store、异步、异常处理
├── dto/              # 会话、消息请求/响应对象
├── resources/
│   ├── knowledge/    # 制度知识库 Markdown 文档
│   └── sql/          # AI 会话/消息建表脚本
└── AiApplication.java

asset-gateway/
├── resources/        # 路由规则、跨域与服务发现配置
└── GatewayApplication.java

asset-common/
├── dto/              # 数据传输对象
├── enums/            # 枚举类
├── security/         # JWT工具类
└── util/             # 公共工具类
```

### 10.2 前端项目结构

```
frontend/src/
├── views/            # 页面视图
├── components/
│   ├── common/       # 通用组件（PageHeader、DataTable、StatusTag 等）
│   └── business/     # 业务组件（申请表单、审批表格、详情弹窗等）
├── stores/           # Pinia 状态管理（用户、权限）
├── styles/           # 设计系统（theme.css：色板、圆角、阴影、动效）
├── router/           # 路由与权限守卫
├── directives/       # 自定义指令（权限控制）
├── config/           # 配置文件
├── utils/            # 工具函数（请求封装、权限判断、图表权限）
├── App.vue
└── main.js
```

---

## 11. 系统特性

### 11.1 核心特性

- **微服务架构**：基于 Spring Cloud Alibaba 的分布式服务架构
- **API 网关**：Spring Cloud Gateway 统一入口,路由转发 + 跨域处理
- **全流程管理**：覆盖资产从采购到报废的完整生命周期
- **多维度折旧**：支持直线法、双倍余额递减法、工作量法
- **智能盘点**：计划制定、任务分配、执行跟踪、结果统计
- **灵活审批**：支持多种审批流程配置
- **实时统计**：多维度的资产数据统计与分析
- **Redis缓存**：资产列表查询引入Redis缓存，性能提升约90%
- **日志追踪**：完整的操作记录与审计追踪

### 11.2 并发控制(三层防御)

针对"同一资产被并发提交领用/报废申请"等场景,采用三层防御保证状态流转不错乱:

| 层级 | 机制 | 防护目标 |
|------|------|---------|
| 第一层 | asset 表 `current_application_id` 字段 + 条件 UPDATE 原子占用 | 源头防止并发申请 |
| 第二层 | `@Version` 乐观锁 + `ObjectOptimisticLockingFailureException` 全局捕获 | 防止审批覆盖 |
| 第三层 | approveApplication 状态机校验(已 finalize 拒绝重复审批,已报废资产拒绝领用) | 兜底越权乱序 |

### 11.3 熔断降级

- Feign 接入 Sentinel,`feign.sentinel.enabled=true` 触发自动接管
- `AuthFeignClient` 配置 `fallbackFactory`,调用失败进入 `AuthFeignClientFallbackFactory`
- 用 FallbackFactory 而非 fallback 属性:能拿到 cause 异常写日志
- 查询类降级返回 503 + 友好提示,统计类降级返回 0,避免阻塞前端

### 11.4 安全特性

- JWT Token 认证
- 基于角色的访问控制（RBAC）
- 跨服务认证与授权
- 操作日志记录
- 跨域访问控制

### 11.5 架构特性

- Nacos 服务注册与发现
- Spring Cloud Gateway 路由转发
- OpenFeign 跨服务调用
- Sentinel 熔断降级
- 服务间 Token 透传
- 统一异常处理
- 统一响应格式

---

## 12. 版本信息

- **项目版本**：0.0.1-SNAPSHOT
- **Spring Boot 版本**：3.2.5
- **Spring Cloud 版本**：2023.0.1
- **Spring Cloud Alibaba 版本**：2023.0.1.0
- **Vue 版本**：3.5.x
- **Element Plus 版本**：2.13.x
- **Vite 版本**：7.x

---

## 13. 许可证

本项目采用 MIT 许可证

# AGENTS.md

## 1. 项目目标
本项目是一个面向 iOS 的 AI 表情图生成应用。用户上传自拍或半身照，选择原创风格模板，后台调用第三方 AI 图像生成 API，返回 2 到 4 张可下载、可分享的表情图或头像图。

本项目的第一优先级不是“功能堆叠”，而是：
1. 生成闭环可用
2. iOS 审核可过
3. IAP 可收款
4. 隐私与内容风控可解释

---

## 2. 产品边界

### 必须坚持
- 产品定位为“原创风格 AI 表情图工具”
- 第一版只做个人生成工具
- 所有 AI 调用必须走服务端代理
- 所有图片与文本在生成前先过审核/风控
- 所有数字权益走 Apple IAP

### 明确禁止
- 不要在模板名、Prompt、营销文案、App 元数据中直接使用未授权 IP 名称
- 不要把第三方 AI API Key 放在客户端
- 不要先做公开社区广场
- 不要先做自训练模型平台
- 不要在未取得明确授权前把用户个人数据发送给第三方 AI

---

## 3. 当前 MVP 范围

### 客户端
- Apple 登录
- 邮箱验证码登录
- 上传图片
- 裁切与压缩
- 模板列表
- 创建生成任务
- 轮询进度
- 展示结果
- 历史记录
- 删除作品
- 下载/分享
- 点数购买
- 恢复购买
- 账号删除
- 隐私政策/用户协议/AI 授权页

### 服务端
- 用户与认证
- 模板管理
- 生成任务状态机
- AI Provider Adapter
- 内容审核与风控
- 媒体资源存储
- IAP 凭证校验
- 点数账户与流水
- Admin 后台

---

## 4. 推荐技术栈

### iOS
- Swift
- SwiftUI
- StoreKit 2
- URLSession
- PhotosUI
- async/await

### Backend
- Java 21
- Spring Boot 3.x
- Spring Security
- PostgreSQL
- Redis
- RabbitMQ 或 Redis Stream
- S3/OSS/R2
- Nginx
- Docker Compose

### 可选
- 图片预处理/后处理可独立成 Python 服务
- 统一日志建议接入 ELK / Loki / Grafana

---

## 5. 目标架构

```text
iOS App
  -> API Gateway / BFF
    -> 用户与认证服务
    -> 模板服务
    -> 生成任务服务
    -> 内容审核与风控
    -> AI Provider Adapter
    -> 支付与权益服务
    -> 媒体资源服务
    -> Admin 后台
```

### 关键原则
- 客户端永远不直连第三方 AI
- Provider Adapter 不允许写死某一家供应商
- 所有任务使用状态机驱动
- 所有失败路径要可追踪、可补偿、可返还权益
- 原图与结果图统一走对象存储

---

## 6. 建议的仓库结构

```text
repo/
  ios-app/
  server/
    gateway/
    user-service/
    template-service/
    generation-service/
    payment-service/
    media-service/
    admin-service/
    common/
  ops/
    docker/
    nginx/
    scripts/
  docs/
    prd/
    api/
    db/
    compliance/
```

如果当前只有单人研发，后端也可以先做成单体：

```text
server-monolith/
  src/main/java/com/company/app/
    auth/
    user/
    template/
    generation/
    payment/
    media/
    admin/
    common/
```

---

## 7. 模块职责

### 7.1 iOS 端
- 登录与账户
- 图片上传与裁切
- 模板展示
- 生成任务轮询
- 结果页
- 历史记录
- 支付与恢复购买
- 设置与法务

### 7.2 后端
- Auth：Apple 登录、邮箱登录、JWT、账号删除
- Template：模板、示例图、参数、启停
- Generation：任务创建、状态机、回调、失败补偿
- Audit：文本审核、图片审核、NSFW、名人/IP 风险
- Provider：统一接入第三方 AI
- Payment：IAP 凭证校验、订单、点数
- Media：对象存储、临时上传凭证、资源元数据
- Admin：模板管理、任务查询、封禁、报表

---

## 8. 状态机约束

生成任务状态固定为：

- CREATED
- AUDITING
- READY_TO_DISPATCH
- RUNNING
- POST_PROCESSING
- SUCCESS
- FAILED
- REFUNDED

规则：
- 状态流转必须单向
- 不允许跳过 AUDITING 直接 RUNNING
- 失败原因必须落库
- 扣点动作必须与 SUCCESS 或预扣返还策略绑定
- webhook 回调要幂等

---

## 9. 数据库约束

### 必备表
- user
- user_auth
- style_template
- style_prompt_rule
- generation_task
- generation_input_asset
- generation_output_asset
- credit_account
- credit_ledger
- iap_order
- receipt_validation_log
- content_audit_log
- app_config
- ai_provider_config

### 命名规则
- 表名使用 snake_case
- 主键统一 bigint 或 uuid（二选一，不要混乱）
- 所有审计表必须带 created_at
- 所有业务主表建议带 updated_at
- 删除行为优先软删除，资源类对象按生命周期清理

---

## 10. API 设计原则

### 接口风格
- REST 为主
- 返回统一结构
- 错误码稳定
- 幂等接口显式支持 idempotency key

### 推荐接口
- POST /api/auth/apple/login
- POST /api/auth/email/send-code
- POST /api/auth/email/login
- POST /api/account/delete
- GET /api/templates
- GET /api/templates/{id}
- POST /api/upload/policy
- POST /api/generations
- GET /api/generations/{taskId}
- GET /api/history
- DELETE /api/history/{id}
- POST /api/iap/verify
- GET /api/credits/balance
- GET /api/config/bootstrap

---

## 11. Provider Adapter 规则

新增或修改 AI 供应商接入时，必须满足：
- 统一请求结构
- 统一结果结构
- 统一错误分类
- 统一超时策略
- 统一成本统计字段
- 统一回调签名校验
- 可按模板或用户分层路由

禁止在业务代码中直接拼接某个供应商的原始请求。

---

## 12. 安全与隐私规则

### 必须遵守
- 图片上传前后都要记录授权状态
- 发送给第三方 AI 前必须可追溯是谁授权的
- 原图保留时间要可配置
- 用户删除账号后要触发资源删除或匿名化流程
- 不允许日志输出用户原图 URL、Token、票据明文

### 高风险点
- 未成年人图像
- 名人/公众人物图像
- 色情化、侮辱性、仇恨性内容
- 利用照片做仿冒、证件伪造或深度伪造

这些必须在审核层拦截或降级。

---

## 13. iOS 审核相关硬性要求

开发过程中默认按以下要求设计：
- 支持账号创建时，必须支持 App 内发起账号删除
- 主要登录若用了第三方/社交登录，要提供等价登录方案
- 数字权益必须走 IAP
- 隐私政策必须能从 App 内访问
- 若向第三方 AI 共享个人数据，必须先取得明确授权
- 第一版不要做公开 UGC 广场

---

## 14. 开发顺序

### 第一优先级
1. 登录与基础配置
2. 图片上传
3. 模板列表
4. 生成任务闭环
5. 结果页与历史记录

### 第二优先级
6. IAP 与点数
7. 账号删除
8. 审核与风控
9. Admin 后台

### 第三优先级
10. 监控与告警
11. 运营位
12. 模板 AB 实验

---

## 15. 测试要求

### 单元测试
- 任务状态机
- 支付凭证校验
- Prompt 编排
- 风控规则

### 集成测试
- 上传 -> 生成 -> 回调 -> 后处理 -> 展示
- IAP 购买 -> 验证 -> 发放权益
- 删除账号 -> 资源清理

### 上架前回归
- 审核账号可登录
- IAP 可见
- Demo 路径可跑通
- 账号删除可找到
- 隐私政策和协议链接可访问

---

## 16. 编码约束

- 不允许提交硬编码密钥
- 不允许把供应商常量散落到多个模块
- 不允许把业务判断写到 Controller
- 所有跨模块 DTO 都要单独定义
- 所有异步任务都要有 traceId
- 所有外部调用都要有 timeout、retry、circuit-breaker 策略

---

## 17. 给开发 Agent 的执行规则

在执行任何任务前，先判断该任务属于：
- iOS 客户端
- 后端接口
- 支付
- 审核风控
- 运维部署
- 合规文档

输出结果时必须说明：
1. 改了哪些文件
2. 为什么这么改
3. 是否影响数据库
4. 是否影响接口
5. 如何验证
6. 有哪些剩余风险

如果任务涉及产品边界冲突（例如要求做某个未授权 IP 风格），默认拒绝直接实现，并改为提供“原创风格替代方案”。

---

## 18. 当前默认决策

- 产品名称方向：原创风格表情图工具
- 第一版平台：iOS
- 计费方式：IAP 点数包优先
- 技术路线：SwiftUI + Spring Boot
- 架构策略：前期单体优先，边界清晰，后期可拆
- AI 策略：第三方 API 主备接入，不先自训模型
- 合规策略：不碰未授权 IP，不做公开社区

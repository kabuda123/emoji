# API Contract Notes

本 iOS 客户端以 monorepo 内的 `../../../docs/api/contract-v1.md` 后端契约为目标。DTO 或接口语义变更前，应先更新该契约源文件，再同步调整客户端模型、请求和测试。

## Base URL

- Local default: `http://localhost:8080`

默认值由 `APIClient` 初始化配置提供。macOS/Xcode 联调时，如果 Simulator 无法访问本机后端，需要按实际网络环境调整 `baseURL`。

## 已接入接口

- `GET /api/config/bootstrap`
- `POST /api/auth/email/send-code`
- `POST /api/auth/email/login`
- `POST /api/auth/apple/login`
- `GET /api/templates`
- `GET /api/templates/{templateID}`
- `POST /api/upload/policy`
- upload policy 返回的 `uploadUrl` PUT 上传
- `POST /api/generations`
- `GET /api/generations/{taskID}`
- `GET /api/history`
- `DELETE /api/history/{taskID}`
- `GET /api/credits/balance`
- `POST /api/iap/verify`

## 后端已提供但客户端未接入

- `POST /api/auth/refresh`
- `POST /api/upload/complete`
- `POST /api/account/delete`
- `GET /api/credits/ledger`

## 当前客户端行为

- 网络层统一解析 `success/data/error/traceId/timestamp` envelope。
- `Authorization` header 由 `SessionStore` 中的 access token 注入。
- 需要幂等保护的生成创建请求会发送 `Idempotency-Key`。
- 图片上传流程当前为 request policy -> 按 policy PUT image data -> 使用 `objectKey` 创建生成任务；尚未调用 `/api/upload/complete`。
- 生成任务创建后按后端返回的 `pollAfterSeconds` 自动轮询，终态停止。
- Email session 和 Apple login session 会写入本地 Keychain-backed store。
- StoreKit 2 购买和恢复购买成功后会调用 `/api/iap/verify`，verify 成功后刷新积分余额。

## 剩余限制

- 当前 Windows 环境没有 `xcodebuild` 或 `swift`，无法完成 XCTest、Simulator build、Release build 和手动主路径验收。
- Sign in with Apple 的 target capability、entitlements 和 signing 仍需在 macOS/Xcode 中确认。
- StoreKit Configuration、App Store Connect 商品配置和消耗型商品恢复行为仍需在 macOS/Xcode 或 Apple 后台验证。
- 认证用户创建生成任务时，后端要求源图已通过 `/api/upload/complete` 确认；客户端补齐该调用前，认证上传到生成的完整主路径可能返回 `409 CONFLICT`。
- 后续接口字段变更需要同步更新 `../../../docs/api/contract-v1.md`、客户端模型、请求和相关测试。

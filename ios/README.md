# emoji-ios

`emoji-ios` 是 AI emoji generation MVP 的 iOS 客户端，位于 monorepo 的 `ios/` 子目录。当前代码围绕后端 `contract-v1` 接口实现模板浏览、图片选择与上传、生成轮询、历史、认证、积分购买和设置页。

## 技术栈

- SwiftUI
- Swift Concurrency / async-await
- URLSession
- PhotosUI
- StoreKit 2
- XCTest

## 本地后端地址

客户端默认后端地址在 `APIClient` 中配置为：

```swift
URL(string: "http://localhost:8080")!
```

本地联调前需要先启动符合 `contract-v1` 的后端服务，并确保 iOS Simulator 可以访问该地址。若后端运行在宿主机但 Simulator 访问失败，需要在 macOS/Xcode 环境中按实际网络拓扑调整 `APIClient.baseURL` 或注入自定义 `APIClient`。

## macOS/Xcode 构建与测试

当前项目需要在 macOS/Xcode 环境执行真实 iOS 构建和测试。Windows 环境没有 `xcodebuild`，只能做静态检查和文档维护。
以下命令默认在 `ios/` 目录执行；如果从仓库根目录执行，将 `EmojiApp.xcodeproj` 改为 `ios/EmojiApp.xcodeproj`。

```bash
xcodebuild -project EmojiApp.xcodeproj -scheme EmojiApp -destination 'platform=iOS Simulator,name=iPhone 16' build
```

```bash
xcodebuild test -project EmojiApp.xcodeproj -scheme EmojiApp -destination 'platform=iOS Simulator,name=iPhone 16'
```

Release build 验证命令：

```bash
xcodebuild -project EmojiApp.xcodeproj -scheme EmojiApp -configuration Release -destination 'generic/platform=iOS' build
```

## GitHub Actions macOS runner

仓库包含 `.github/workflows/ios.yml` 后，可通过 GitHub Actions 的 `macos-15` runner 执行真实 iOS 构建与 XCTest。该 workflow 会：

- 选择可用的 iPhone Simulator。
- 执行 Debug Simulator build。
- 执行 XCTest。
- 执行 Release iOS SDK build，并通过 `CODE_SIGNING_ALLOWED=NO` 跳过 CI 环境中的开发者签名。

workflow 会在修改 `ios/**` 或 `.github/workflows/ios.yml` 的 push、pull request，以及手动 `workflow_dispatch` 时运行。

## 个人 iPhone 真机测试

当前 Windows 本机不能直接把 iOS app 打包安装到 iPhone。个人真机测试需要在 macOS/Xcode 环境完成：

1. 在 Mac 上安装 Xcode，并确认命令行工具可用。

```bash
xcodebuild -version
xcrun simctl list devices available
```

2. 用 Xcode 打开 `EmojiApp.xcodeproj`，在 target 的 `Signing & Capabilities` 中选择个人 Apple ID 对应的 Team，并设置唯一的 Bundle Identifier。
3. 用 USB 连接 iPhone，按 Xcode 提示信任设备和开发者证书。
4. 在 Xcode 顶部设备列表选择该 iPhone，执行 Run。
5. 若要验证 Sign in with Apple、StoreKit 2 或 IAP，仍需在 Xcode capability、StoreKit Configuration、App Store Connect 商品和 Apple Developer 账号能力上补齐对应配置。

真机部署前建议先在 Mac 上通过本项目的 build/test 命令；当前项目在 Windows 环境下尚未完成真实 Swift 编译验证。

## 当前接入范围

- Bootstrap config 加载与设置页展示
- Email login
- Sign in with Apple 授权入口与后端登录请求
- Template list/detail
- PhotosUI 图片选择、upload policy 请求、按 policy PUT 上传
- Generation create/detail polling
- History list/delete
- Credit balance
- StoreKit 2 product loading、purchase、restore、transaction updates
- IAP verify 与购买后余额刷新
- Settings、legal documents、session 清理

## 当前未接入的后端能力

- `/api/upload/complete`：后端已要求认证用户创建生成任务前源图处于 `UPLOADED` 或 `ATTACHED` 状态；当前 `ImageUploadService` 只执行 upload policy 和 presigned PUT，尚未调用上传完成确认。认证生成主路径联调前需要补齐这一调用。
- `/api/auth/refresh`：后端已支持 refresh token 轮换；当前客户端保存 refresh token，但尚未实现 access token 自动刷新。
- `/api/account/delete`：后端已支持账号删除请求；当前客户端设置页尚未接入。
- `/api/credits/ledger`：后端已支持用户积分流水分页；当前客户端只读取积分余额。

## StoreKit Configuration

支付页通过 `StoreKitPurchaseService` 加载 `PurchaseProductCatalog.defaultProductIDs` 中的商品，当前默认商品包含 `credits_120`。在 macOS/Xcode 手动验证购买与恢复购买时，需要：

- 在 Xcode 中为 scheme 绑定匹配的 StoreKit Configuration。
- 在 StoreKit Configuration 中创建同名商品 id。
- 验证购买成功后 `/api/iap/verify` 被调用，并随后刷新 `/api/credits/balance`。
- 验证 `Restore Purchases` 可以枚举历史交易并重新执行 verify。

`Info.plist` 已包含 `SKIncludeConsumableInAppPurchaseHistory`，用于让 `Transaction.all` 包含消耗型商品历史；后端仍需要通过订单或 transaction id 保持 verify 幂等。

## 文档

- API 合约源文件：`../docs/api/contract-v1.md`
- iOS API contract notes：`docs/api-contract/README.md`

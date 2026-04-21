# qr-float-login 接入文档

适用场景：

- uni-app / HBuilderX 项目
- Android 端
- 需要悬浮窗 + 跨应用截图 + 二维码识别

## 1. 插件目录接入

把整个目录复制到目标项目：

```text
uni_modules/qr-float-login
```

复制后建议保留以下结构：

```text
uni_modules
└── qr-float-login
    ├── index.uts
    ├── package.json
    ├── readme.md
    └── utssdk
        ├── interface.uts
        └── app-android
            ├── AndroidManifest.xml
            ├── config.json
            ├── FloatWindowService.kt
            ├── index.uts
            ├── ProjectionController.kt
            ├── QrDecoder.kt
            └── QrFloatNative.kt
```

## 2. manifest.json 配置

在目标项目的 [manifest.json](/Users/hans/Documents/Playground/hbuilder-floating-qr-demo/manifest.json#L1) 中补充 Android 权限：

```json
{
  "app-plus": {
    "distribute": {
      "android": {
        "permissions": [
          "<uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\"/>",
          "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\"/>",
          "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION\"/>",
          "<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\"/>"
        ]
      }
    }
  }
}
```

说明：

- `SYSTEM_ALERT_WINDOW`：悬浮窗
- `FOREGROUND_SERVICE`：前台服务
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`：Android 14+ 截屏前台服务类型
- `POST_NOTIFICATIONS`：Android 13+ 前台服务通知

## 3. 运行环境要求

这个插件不能只用标准调试基座。

请使用：

1. HBuilderX
2. Android 自定义调试基座
3. 真机测试

因为插件包含：

- UTS Android 原生代码
- 前台服务
- `MediaProjection`
- ML Kit 三方依赖

## 4. 页面中引入插件

推荐在页面里运行时加载：

```js
export default {
  data() {
    return {
      qrFloatLogin: null
    }
  },
  onLoad() {
    this.qrFloatLogin = require('@/uni_modules/qr-float-login')
  }
}
```

## 5. 推荐接入时序

标准时序：

1. 申请悬浮窗权限
2. 申请截屏权限
3. 启动悬浮窗
4. 用户切到目标 App
5. 在悬浮窗中先确认“已退出个人账号”
6. 点击“点我上号”
7. 插件截图并识别二维码
8. 页面接收识别结果并处理登录逻辑

## 6. 最小调用示例

```vue
<script>
export default {
  data() {
    return {
      qrFloatLogin: null,
      qrResult: '',
      statusText: ''
    }
  },
  onLoad() {
    this.qrFloatLogin = require('@/uni_modules/qr-float-login')
    this.qrFloatLogin.onQrDetected(this.handleDetected)
    this.qrFloatLogin.onError(this.handleStatus)
  },
  onUnload() {
    if (!this.qrFloatLogin) {
      return
    }
    this.qrFloatLogin.offQrDetected(this.handleDetected)
    this.qrFloatLogin.offError(this.handleStatus)
  },
  methods: {
    async requestOverlay() {
      const granted = await this.qrFloatLogin.requestOverlayPermission()
      this.statusText = granted ? '悬浮窗权限已授予' : '悬浮窗权限未授予'
    },
    async requestCapture() {
      const granted = await this.qrFloatLogin.requestScreenCapturePermission()
      this.statusText = granted ? '截屏权限已授予' : '截屏权限未授予'
    },
    async startFloat() {
      const ok = await this.qrFloatLogin.startFloatWindow()
      this.statusText = ok ? '悬浮窗已启动' : '悬浮窗启动失败'
    },
    async stopFloat() {
      await this.qrFloatLogin.stopFloatWindow()
      this.statusText = '悬浮窗已关闭'
    },
    handleDetected(payload) {
      this.qrResult = payload.text || ''
      this.statusText = '识别成功'
    },
    handleStatus(message) {
      this.statusText = message || '发生错误'
    }
  }
}
</script>
```

## 7. 插件对外 API

主要接口：

- `checkOverlayPermission(): boolean`
- `requestOverlayPermission(): Promise<boolean>`
- `hasScreenCapturePermission(): boolean`
- `requestScreenCapturePermission(): Promise<boolean>`
- `startFloatWindow(): Promise<boolean>`
- `stopFloatWindow(): Promise<void>`
- `captureAndDecodeOnce(): Promise<QrPayload | null>`
- `getLatestResult(): QrPayload | null`
- `getLatestStatusMessage(): string`
- `getLatestCapturePath(): string`
- `clearLatestResult(): void`
- `onQrDetected(callback)`
- `offQrDetected(callback)`
- `onError(callback)`
- `offError(callback)`

返回结果结构：

```ts
type QrPayload = {
  text: string
  format: string
  timestamp: number
}
```

## 8. 识别结果处理建议

推荐在 `onQrDetected` 中处理：

- 二维码内容回填
- 调业务登录接口
- 跳转到业务页面

也可以通过以下接口做调试：

- `getLatestCapturePath()`：获取最近一次缓存截图路径
- `getLatestStatusMessage()`：获取最近状态

## 9. 已知限制

- 仅支持 Android
- 必须真机测试
- 必须用户显式授权截屏
- 部分 App 会被系统或 ROM 保护，导致截不到画面
- 某些系统在录屏授权页需要勾选完整屏幕共享相关选项
- 若目标页面设置了 `FLAG_SECURE`，系统会阻止截图

常见被保护场景：

- 微信
- QQ
- 部分支付、金融、聊天类 App

## 10. 常见问题

### Q1：为什么标准基座里悬浮窗不显示？

因为标准基座不适合调试这类带原生服务与三方依赖的插件，请使用自定义调试基座。

### Q2：为什么点击悬浮窗没有识别结果？

优先检查：

1. 是否已经完成悬浮窗权限授权
2. 是否已经完成截屏权限授权
3. 目标 App 是否受系统保护
4. 系统录屏授权页是否勾选了完整共享相关选项

### Q3：为什么需要前台服务通知？

因为 Android 对 `MediaProjection` 和后台长时任务有前台服务要求。

## 11. 推荐接入方式

在业务项目里建议二次封装一层，例如：

- `services/float-login.js`
- `composables/useFloatLogin.js`
- `utils/qr-float-login.js`

把插件调用、权限申请、错误提示、识别结果处理都集中管理，页面只保留业务逻辑。

## 12. 当前 Demo 参考

可参考当前 demo 页面接入方式：

- [index.vue](/Users/hans/Documents/Playground/hbuilder-floating-qr-demo/pages/index/index.vue#L1)

可参考插件接口定义：

- [interface.uts](/Users/hans/Documents/Playground/hbuilder-floating-qr-demo/uni_modules/qr-float-login/utssdk/interface.uts#L1)

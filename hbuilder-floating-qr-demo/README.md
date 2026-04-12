# HBuilder Floating QR Demo

这是一个基于 `uni-app` + `UTS Android 插件` 的 demo，用于演示：

- 申请悬浮窗权限
- 申请屏幕捕获权限
- 启动悬浮窗按钮
- 在其他应用页面点击悬浮窗后抓取当前屏幕
- 识别二维码并把结果回传给 `uni-app` 页面

## 目录结构

```text
hbuilder-floating-qr-demo
├── App.vue
├── main.js
├── manifest.json
├── pages.json
├── package.json
├── pages
│   └── index
│       └── index.vue
└── uni_modules
    └── qr-float-login
        ├── package.json
        ├── readme.md
        └── utssdk
            ├── interface.uts
            └── app-android
                ├── AndroidManifest.xml
                ├── FloatWindowService.kt
                ├── index.uts
                ├── ProjectionController.kt
                ├── QrDecoder.kt
                └── QrFloatNative.kt
```

## 使用方式

1. 用 HBuilderX 导入该目录。
2. 通过 `manifest.json` 配置 AppID、签名等实际信息。
3. 到 HBuilderX `设置 -> 运行配置 -> Android 运行环境` 配置自定义调试基座。
4. 制作自定义基座后运行到 Android 真机。
5. 注意：本 demo 依赖 `com.google.mlkit:barcode-scanning:17.3.0`，标准调试基座无法联调该三方依赖。
6. 在首页依次点击：
   - `申请悬浮窗权限`
   - `申请截屏权限`
   - `启动悬浮窗`
7. 切到目标应用二维码页面，点击悬浮球执行识别。
8. 切回 App 首页，页面会同步最近一次识别结果。

## 说明

- 该 demo 依赖 Android `MediaProjection`，必须由用户授权。
- 如果目标页面设置了 `FLAG_SECURE`，系统会阻止截图。
- Android 13+ 建议同时处理通知权限，否则前台服务通知体验较差。
- 这是一份可落地的最小骨架，实际商用前还需要补充机型兼容、异常兜底和合规提示。
- 如果 HBuilderX 控制台提示“存在三方依赖或资源引用”，需要先切换到自定义调试基座再运行。

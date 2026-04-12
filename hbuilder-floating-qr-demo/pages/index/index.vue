<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">Android / HBuilderX</text>
      <text class="title">跨应用二维码截图登录 Demo</text>
      <text class="desc">
        先授权悬浮窗和截屏，再启动悬浮球。授权完成后会建立持续截屏会话，之后在其他 App 点击悬浮窗即可直接识别。
      </text>
    </view>

    <view class="card">
      <button class="action" @click="requestOverlay">申请悬浮窗权限</button>
      <button class="action" @click="requestCapture">申请截屏权限</button>
      <button class="action primary" @click="startFloat">启动悬浮窗</button>
      <button class="action" @click="stopFloat">关闭悬浮窗</button>
      <button class="action ghost" @click="decodeOnce">在当前 App 内测试识别一次</button>
    </view>
	<!-- <text class="label">最近截图预览</text><!-- -->
	<!-- <view v-if="capturePath" class="preview-box" @click="openPreview"> -->
	  <!-- <image class="capture-image" :src="capturePath" mode="widthFix"></image> -->
	  <!-- <text class="preview-tip">点击查看截图详情</text> -->
	<!-- </view> -->
	<!-- <text v-else class="result">暂无截图</text -->

    <view class="card">
      <text class="label">状态</text>
      <text class="status">{{ statusText }}</text>
      <text class="label">最近识别结果</text>
      <text class="result">{{ qrResult || '暂无结果' }}</text>
      
      <text v-if="capturePath" class="path">{{ capturePath }}</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      statusText: '页面已加载，等待授权',
      qrResult: '',
      capturePath: '',
      qrFloatLogin: null
    }
  },
  onLoad() {
    this.initPlugin()
    this.bindPluginEvents()
    this.syncLatestResult()
  },
  onShow() {
    this.syncLatestResult()
  },
  methods: {
    initPlugin() {
      try {
        this.qrFloatLogin = require('@/uni_modules/qr-float-login')
      } catch (error) {
        this.statusText = '插件加载失败'
        console.error('initPlugin error', error)
      }
    },
    bindPluginEvents() {
      if (!this.qrFloatLogin) {
        return
      }
      try {
        this.qrFloatLogin.onQrDetected(this.handleDetected)
        this.qrFloatLogin.onError(this.handleError)
      } catch (error) {
        this.statusText = '插件事件绑定失败'
        console.error('bindPluginEvents error', error)
      }
    },
    unbindPluginEvents() {
      if (!this.qrFloatLogin) {
        return
      }
      try {
        this.qrFloatLogin.offQrDetected(this.handleDetected)
        this.qrFloatLogin.offError(this.handleError)
      } catch (error) {
        console.error('unbindPluginEvents error', error)
      }
    },
    handleDetected(payload) {
      this.qrResult = (payload && payload.text) || ''
      this.statusText = '识别成功'
      uni.showToast({
        title: '识别到二维码',
        icon: 'success'
      })
    },
    handleError(message) {
      this.statusText = message || '发生错误'
      uni.showToast({
        title: message || '操作失败',
        icon: 'none'
      })
    },
    syncLatestResult() {
      if (!this.qrFloatLogin) {
        return
      }
      try {
        const statusMessage = this.qrFloatLogin.getLatestStatusMessage()
        if (statusMessage) {
          this.statusText = statusMessage
        }
        const capturePath = this.qrFloatLogin.getLatestCapturePath()
        if (capturePath) {
          this.capturePath = capturePath
        }
        const payload = this.qrFloatLogin.getLatestResult()
        if (payload && payload.text) {
          this.qrResult = payload.text
          this.statusText = '检测到最近一次识别结果'
          this.qrFloatLogin.clearLatestResult()
        }
      } catch (error) {
        console.error('syncLatestResult error', error)
      }
    },
    async requestOverlay() {
      if (!this.qrFloatLogin) {
        this.statusText = '插件未加载'
        return
      }
      try {
        const granted = await this.qrFloatLogin.requestOverlayPermission()
        this.statusText = granted ? '悬浮窗权限已授予' : '悬浮窗权限未授予'
      } catch (error) {
        this.statusText = '申请悬浮窗权限失败'
        console.error('requestOverlay error', error)
      }
    },
    async requestCapture() {
      if (!this.qrFloatLogin) {
        this.statusText = '插件未加载'
        return
      }
      try {
        const granted = await this.qrFloatLogin.requestScreenCapturePermission()
        this.statusText = granted ? '截屏权限已授予' : '截屏权限未授予'
      } catch (error) {
        this.statusText = '申请截屏权限失败'
        console.error('requestCapture error', error)
      }
    },
    async startFloat() {
      if (!this.qrFloatLogin) {
        this.statusText = '插件未加载'
        return
      }
      try {
        const ok = await this.qrFloatLogin.startFloatWindow()
        this.statusText = ok ? '悬浮窗启动请求已发送' : '悬浮窗启动失败'
      } catch (error) {
        this.statusText = '启动悬浮窗失败'
        console.error('startFloat error', error)
      }
    },
    async stopFloat() {
      if (!this.qrFloatLogin) {
        this.statusText = '插件未加载'
        return
      }
      try {
        await this.qrFloatLogin.stopFloatWindow()
        this.statusText = '悬浮窗已关闭'
      } catch (error) {
        this.statusText = '关闭悬浮窗失败'
        console.error('stopFloat error', error)
      }
    },
    async decodeOnce() {
      if (!this.qrFloatLogin) {
        this.statusText = '插件未加载'
        return
      }
      try {
        const result = await this.qrFloatLogin.captureAndDecodeOnce()
        this.qrResult = (result && result.text) || ''
        this.statusText = result && result.text ? '当前页识别成功' : '未识别到二维码'
      } catch (error) {
        this.statusText = '识别失败'
        const message =
          error && typeof error === 'object' && error.message
            ? error.message
            : '识别失败'
        uni.showToast({
          title: message,
          icon: 'none'
        })
      }
    },
    openPreview() {
      if (!this.capturePath) {
        return
      }
      uni.navigateTo({
        url:
          '/pages/preview/index?capturePath=' +
          encodeURIComponent(this.capturePath) +
          '&qrResult=' +
          encodeURIComponent(this.qrResult || '') +
          '&statusText=' +
          encodeURIComponent(this.statusText || '')
      })
    }
  },
  onUnload() {
    this.unbindPluginEvents()
  }
}
</script>

<style>
.page {
  min-height: 100vh;
  padding: 36rpx 28rpx 48rpx;
  background:
    radial-gradient(circle at top right, rgba(218, 166, 74, 0.22), transparent 34%),
    linear-gradient(180deg, #f9f4e8 0%, #efe6d2 100%);
}

.hero {
  padding: 30rpx 6rpx 36rpx;
}

.eyebrow {
  display: block;
  margin-bottom: 16rpx;
  color: #8e5d22;
  font-size: 24rpx;
  letter-spacing: 2rpx;
}

.title {
  display: block;
  font-size: 52rpx;
  line-height: 1.2;
  font-weight: 700;
  color: #2d2416;
}

.desc {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #544a38;
}

.card {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 251, 244, 0.9);
  box-shadow: 0 18rpx 40rpx rgba(79, 58, 22, 0.08);
}

.action {
  margin-top: 18rpx;
  border-radius: 999rpx;
  background: #ead9b4;
  color: #3b2d12;
}

.action.primary {
  background: #2f7d57;
  color: #ffffff;
}

.action.ghost {
  background: #f3ede0;
  color: #6c5b3e;
}

.label {
  display: block;
  margin-top: 10rpx;
  color: #8c7754;
  font-size: 24rpx;
}

.status {
  display: block;
  margin-top: 10rpx;
  font-size: 32rpx;
  color: #2a2419;
}

.result {
  display: block;
  margin-top: 10rpx;
  min-height: 120rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #f3ecdf;
  color: #2a2419;
  word-break: break-all;
}

.capture-image {
  width: 100%;
  border-radius: 18rpx;
  background: #f3ecdf;
}

.preview-box {
  margin-top: 12rpx;
}

.preview-tip {
  display: block;
  margin-top: 12rpx;
  text-align: center;
  color: #2f7d57;
  font-size: 24rpx;
}

.path {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: #6c5b3e;
  word-break: break-all;
}
</style>

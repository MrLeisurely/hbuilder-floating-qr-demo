<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">Capture Preview</text>
      <text class="title">截图预览详情</text>
      <text class="desc">这里展示最近一次缓存截图、识别结果和本地路径，方便你排查截图与识别是否符合预期。</text>
    </view>

    <view class="card">
      <text class="label">截图预览</text>
      <image v-if="capturePath" class="capture-image" :src="capturePath" mode="widthFix"></image>
      <view v-else class="empty">
        <text class="empty-text">暂无截图可预览</text>
      </view>
    </view>

    <view class="card">
      <text class="label">识别结果</text>
      <text class="result">{{ qrResult || '暂无识别结果' }}</text>

      <text class="label">最近状态</text>
      <text class="result">{{ statusText || '暂无状态信息' }}</text>

      <text class="label">缓存路径</text>
      <text class="path">{{ capturePath || '暂无路径' }}</text>
    </view>

    <view class="card actions">
      <button class="action primary" @click="previewImage" :disabled="!capturePath">查看大图</button>
      <button class="action" @click="goBack">返回首页</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      capturePath: '',
      qrResult: '',
      statusText: ''
    }
  },
  onLoad(options) {
    this.capturePath = decodeURIComponent(options.capturePath || '')
    this.qrResult = decodeURIComponent(options.qrResult || '')
    this.statusText = decodeURIComponent(options.statusText || '')
  },
  methods: {
    previewImage() {
      if (!this.capturePath) {
        return
      }
      uni.previewImage({
        urls: [this.capturePath],
        current: this.capturePath
      })
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style>
.page {
  min-height: 100vh;
  padding: 36rpx 28rpx 48rpx;
  background:
    radial-gradient(circle at top right, rgba(218, 166, 74, 0.18), transparent 34%),
    linear-gradient(180deg, #f9f4e8 0%, #efe6d2 100%);
}

.hero {
  padding: 20rpx 6rpx 30rpx;
}

.eyebrow {
  display: block;
  margin-bottom: 14rpx;
  color: #8e5d22;
  font-size: 24rpx;
  letter-spacing: 2rpx;
}

.title {
  display: block;
  font-size: 48rpx;
  line-height: 1.2;
  font-weight: 700;
  color: #2d2416;
}

.desc {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #544a38;
}

.card {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 251, 244, 0.94);
  box-shadow: 0 18rpx 40rpx rgba(79, 58, 22, 0.08);
}

.label {
  display: block;
  margin-top: 10rpx;
  color: #8c7754;
  font-size: 24rpx;
}

.capture-image {
  width: 100%;
  margin-top: 16rpx;
  border-radius: 20rpx;
  background: #f3ecdf;
}

.result {
  display: block;
  margin-top: 10rpx;
  min-height: 80rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f3ecdf;
  color: #2a2419;
  word-break: break-all;
  line-height: 1.6;
}

.path {
  display: block;
  margin-top: 10rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f3ecdf;
  color: #6c5b3e;
  font-size: 24rpx;
  line-height: 1.6;
  word-break: break-all;
}

.empty {
  margin-top: 16rpx;
  padding: 48rpx 24rpx;
  border-radius: 20rpx;
  background: #f3ecdf;
}

.empty-text {
  display: block;
  text-align: center;
  color: #6c5b3e;
  font-size: 28rpx;
}

.actions {
  display: flex;
  flex-direction: column;
}

.action {
  margin-top: 16rpx;
  border-radius: 999rpx;
  background: #ead9b4;
  color: #3b2d12;
}

.action.primary {
  background: #2f7d57;
  color: #ffffff;
}
</style>

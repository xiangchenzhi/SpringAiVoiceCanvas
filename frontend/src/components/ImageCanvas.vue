<template>
  <div class="image-canvas">
    <div v-if="!imageUrl && !loading" class="empty-state">
      <div class="empty-icon">🖼️</div>
      <p>输入描述，AI 将为你的创意生成图片</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p v-if="loadingStep === 'enhancing'">正在优化提示词...</p>
      <p v-else>正在生成图片...</p>
    </div>

    <div v-if="imageUrl && !loading" class="image-result">
      <img :src="imageUrl" alt="AI Generated" class="generated-img" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  imageUrl: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  loadingStep: { type: String, default: '' }
})
</script>

<style scoped>
.image-canvas {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  overflow: auto;
}
.empty-state {
  text-align: center;
  color: #94a3b8;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { font-size: 14px; }

.loading-state {
  text-align: center;
  color: #64748b;
}
.loading-spinner {
  width: 40px; height: 40px;
  margin: 0 auto 16px;
  border: 3px solid #e2e8f0;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-state p { font-size: 14px; }

.image-result {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.generated-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
</style>

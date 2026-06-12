<template>
  <div class="voice-control">
    <button
      class="mic-btn"
      :class="{ listening: isListening }"
      @mousedown="start"
      @mouseup="stop"
      @mouseleave="stop"
      @touchstart.prevent="start"
      @touchend.prevent="stop"
    >
      <span class="mic-icon">{{ isListening ? '🎙️' : '🎤' }}</span>
      <span class="btn-text">{{ isListening ? '正在聆听...松手结束' : '按住说话' }}</span>
    </button>
    <div v-if="transcript" class="transcript-display">
      <span class="label">识别结果：</span>
      <span class="text">{{ transcript }}</span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  isListening: Boolean,
  transcript: String
})

const emit = defineEmits(['start', 'stop'])

function start() {
  emit('start')
}

function stop() {
  if (props.isListening) {
    emit('stop')
  }
}
</script>

<style scoped>
.voice-control {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.mic-btn {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 3px solid #00d4ff;
  background: #16213e;
  color: #eee;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.2s;
  user-select: none;
}
.mic-btn:hover {
  background: #1a2756;
}
.mic-btn.listening {
  background: #e94560;
  border-color: #e94560;
  animation: pulse 1.2s infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}
.mic-icon {
  font-size: 48px;
}
.btn-text {
  font-size: 14px;
}
.transcript-display {
  background: #16213e;
  border-radius: 6px;
  padding: 10px 20px;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 500px;
}
.label {
  color: #888;
  font-size: 13px;
  white-space: nowrap;
}
.text {
  color: #00d4ff;
  font-size: 15px;
}
</style>

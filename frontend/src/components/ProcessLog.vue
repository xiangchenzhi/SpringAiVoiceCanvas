<template>
  <div class="process-log">
    <div class="log-title">AI 执行过程</div>
    <div class="log-entries" ref="logRef">
      <div
        v-for="(entry, i) in entries"
        :key="i"
        :class="['log-line', entry.type]"
      >
        <span class="log-dot" :class="entry.type"></span>
        <span class="log-msg">{{ entry.msg }}</span>
      </div>
      <div v-if="entries.length === 0" class="log-empty">
        输入指令后在此查看 AI 执行过程
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  entries: { type: Array, default: () => [] }
})

const logRef = ref(null)

watch(() => props.entries.length, async () => {
  await nextTick()
  if (logRef.value) {
    logRef.value.scrollTop = logRef.value.scrollHeight
  }
})
</script>

<style scoped>
.process-log {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
}
.log-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}
.log-entries {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.log-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.5;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.log-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.log-dot.info  { background: #6366f1; }
.log-dot.success { background: #22c55e; }
.log-dot.node  { background: #f59e0b; }
.log-dot.error { background: #ef4444; }
.log-line.node { padding-left: 12px; }
.log-line.node .log-msg { color: #94a3b8; }
.log-line.success .log-msg { color: #22c55e; }
.log-line.error .log-msg { color: #ef4444; }
.log-msg { color: #cbd5e1; }
.log-empty {
  color: #475569;
  font-size: 13px;
  text-align: center;
  padding: 24px 0;
}
</style>

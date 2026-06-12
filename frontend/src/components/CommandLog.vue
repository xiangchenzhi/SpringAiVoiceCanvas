<template>
  <div v-if="commands.length > 0" class="command-log">
    <h3>命令记录</h3>
    <div class="log-list">
      <div
        v-for="(entry, i) in commands"
        :key="i"
        class="log-entry"
      >
        <div class="log-header">
          <span class="log-time">{{ entry.time }}</span>
          <span class="log-count">{{ entry.commands.length }} 条命令</span>
        </div>
        <div class="log-text">"{{ entry.text }}"</div>
        <div class="log-commands">
          <span
            v-for="(cmd, j) in entry.commands"
            :key="j"
            class="cmd-tag"
          >{{ cmd.action }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  commands: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.command-log {
  width: 100%;
  max-width: 1000px;
  background: #16213e;
  border-radius: 8px;
  padding: 16px;
  margin-top: 8px;
}
h3 {
  font-size: 16px;
  color: #aaa;
  margin-bottom: 12px;
}
.log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
}
.log-entry {
  background: #0f0f23;
  border-radius: 6px;
  padding: 10px 14px;
}
.log-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.log-time {
  color: #666;
  font-size: 12px;
}
.log-count {
  color: #888;
  font-size: 12px;
}
.log-text {
  color: #00d4ff;
  font-size: 14px;
  margin-bottom: 8px;
}
.log-commands {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.cmd-tag {
  background: #1a3a5c;
  color: #5cc8ff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}
</style>

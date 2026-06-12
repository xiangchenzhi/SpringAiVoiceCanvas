<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <span class="sidebar-logo">DiagramGPT</span>
      <button class="new-chat-btn" @click="$emit('new-conversation')" title="新建会话">
        <span>+</span> 新会话
      </button>
    </div>
    <div class="sidebar-list">
      <div
        v-for="c in conversations"
        :key="c.id"
        :class="['sidebar-item', { active: activeId === c.id }]"
        @click="$emit('select', c.id)"
      >
        <div class="item-type">
          <span class="type-dot" :class="'dot-' + (c.lastType || 'image')"></span>
        </div>
        <div class="item-body">
          <div class="item-title">{{ c.title || '未命名会话' }}</div>
          <div class="item-meta">{{ labelForType(c.conversationType) }} · {{ timeAgo(c.updatedAt) }}</div>
        </div>
      </div>
      <div v-if="conversations.length === 0" class="sidebar-empty">
        暂无历史会话
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: String, default: '' }
})

defineEmits(['select', 'new-conversation'])

function labelForType(t) {
  const m = { DIAGRAM: '图表生成', IMAGE: 'AI 绘画' }
  return m[t] || '混合会话'
}

function timeAgo(ts) {
  if (!ts) return ''
  const diff = Date.now() - new Date(ts).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}
</script>

<style scoped>
.sidebar {
  width: 240px; min-width: 240px; height: 100vh;
  background: #0f172a; border-right: 1px solid #1e293b;
  display: flex; flex-direction: column; overflow: hidden;
}
.sidebar-header {
  padding: 14px 16px; display: flex; flex-direction: column; gap: 10px;
  border-bottom: 1px solid #1e293b; flex-shrink: 0;
}
.sidebar-logo {
  font-size: 18px; font-weight: 700;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.new-chat-btn {
  display: flex; align-items: center; justify-content: center; gap: 4px;
  width: 100%; padding: 8px 0; border-radius: 8px;
  border: 1px solid #334155; background: #1e293b;
  color: #cbd5e1; font-size: 13px; cursor: pointer;
  transition: border-color 0.2s;
}
.new-chat-btn:hover { border-color: #60a5fa; color: #e2e8f0; }
.new-chat-btn span { font-size: 16px; }
.sidebar-list { flex: 1; overflow-y: auto; padding: 8px; }
.sidebar-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px; border-radius: 8px; cursor: pointer;
  transition: background 0.15s; margin-bottom: 2px;
}
.sidebar-item:hover { background: #1e293b; }
.sidebar-item.active { background: #1e3a5f; }
.type-dot {
  display: block; width: 8px; height: 8px; border-radius: 50%; margin-top: 5px;
}
.dot-image { background: #a78bfa; }
.dot-diagram { background: #60a5fa; }
.dot-shape { background: #f59e0b; }
.item-body { flex: 1; min-width: 0; }
.item-title { font-size: 13px; color: #e2e8f0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-meta { font-size: 11px; color: #64748b; margin-top: 2px; }
.sidebar-empty { text-align: center; color: #475569; font-size: 13px; padding: 24px 0; }
</style>

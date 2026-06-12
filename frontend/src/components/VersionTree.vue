<template>
  <Teleport to="body">
    <Transition name="popover-fade">
      <div v-if="visible" class="vpopover-backdrop" @click.self="$emit('close')">
        <div class="vpopover">
          <div class="vpopover-header">
            <div class="vpopover-title-group">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/></svg>
              <span>版本历史</span>
              <span class="vpopover-count">{{ flatCount }} 个版本</span>
            </div>
            <button class="vpopover-close" @click="$emit('close')">&times;</button>
          </div>
          <div class="vpopover-body">
            <div v-if="!tree || tree.length === 0" class="vpopover-empty">
              暂无历史版本，与 AI 对话后自动生成
            </div>
            <div v-else class="vtree-canvas">
              <VersionTreeNode
                v-for="(root, index) in tree"
                :key="root.id"
                :node="root"
                :active-id="activeId"
                :selected-id="selectedId"
                :is-root="true"
                :is-last="index === tree.length - 1"
                @select="$emit('select-node', $event)"
                @continue="$emit('continue-from', $event)"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import VersionTreeNode from './VersionTreeNode.vue'

const props = defineProps({
  visible: Boolean,
  tree: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  selectedId: { type: String, default: '' }
})

defineEmits(['close', 'select-node', 'continue-from'])

const flatCount = computed(() => {
  function count(nodes) {
    let c = 0
    for (const n of nodes) {
      c++
      if (n.children?.length) c += count(n.children)
    }
    return c
  }
  return count(props.tree)
})
</script>

<style scoped>
.popover-fade-enter-active { transition: all 0.2s ease-out; }
.popover-fade-leave-active { transition: all 0.15s ease-in; }
.popover-fade-enter-from,
.popover-fade-leave-to { opacity: 0; }
.popover-fade-enter-from .vpopover,
.popover-fade-leave-to .vpopover { transform: scale(0.96) translateY(-10px); }

.vpopover-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.55); display: flex;
  align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.vpopover {
  width: 760px; max-width: 92vw; max-height: 84vh;
  background:
    radial-gradient(circle at top left, rgba(59,130,246,0.08), transparent 28%),
    radial-gradient(circle at top right, rgba(139,92,246,0.08), transparent 24%),
    #131a27;
  border: 1px solid #263246;
  border-radius: 20px; box-shadow: 0 32px 100px rgba(0,0,0,0.55);
  display: flex; flex-direction: column; overflow: hidden;
  transition: transform 0.2s ease-out;
}
.vpopover-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #2d3348; flex-shrink: 0;
}
.vpopover-title-group {
  display: flex; align-items: center; gap: 10px; color: #e2e8f0;
  font-size: 15px; font-weight: 600;
}
.vpopover-title-group svg { color: #60a5fa; }
.vpopover-count {
  font-size: 12px; font-weight: 400; color: #64748b;
  background: #22283a; padding: 2px 8px; border-radius: 10px;
}
.vpopover-close {
  width: 32px; height: 32px; border: none; border-radius: 8px;
  background: transparent; color: #94a3b8; font-size: 20px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.vpopover-close:hover { background: #2d3348; color: #e2e8f0; }
.vpopover-body {
  flex: 1; overflow-y: auto; padding: 18px 22px 28px;
}
.vpopover-empty {
  text-align: center; color: #64748b; font-size: 14px; padding: 32px 0;
}
.vtree-canvas {
  padding: 2px 4px 12px;
}
</style>

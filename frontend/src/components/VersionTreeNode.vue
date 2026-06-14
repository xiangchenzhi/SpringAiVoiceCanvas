<template>
  <div class="tree-node">
    <div class="node-row">
      <div class="node-rail" :class="{ root: isRoot, last: isLast }">
        <div v-if="!isRoot" class="rail-vertical"></div>
        <div v-if="!isRoot" class="rail-horizontal"></div>
        <div class="rail-dot" :class="{ active: isActive }"></div>
      </div>

      <div
        class="node-card"
        :class="{ active: isActive, selected: isSelected, branching: isBranchingFrom }"
        @click="$emit('select', node.id)"
      >
        <div class="node-main">
          <div class="node-top">
            <span class="node-title">{{ summaryText }}</span>
            <span v-if="isRoot" class="node-badge root-badge">ROOT</span>
            <span v-else-if="hasChildren" class="node-badge branch-badge">{{ node.children.length }} 分支</span>
            <span v-else class="node-badge leaf-badge">叶子</span>
          </div>
          <div v-if="node.command" class="node-command">{{ node.command }}</div>
        </div>

        <div class="node-actions">
          <span v-if="isActive" class="current-badge">当前</span>
          <span v-else-if="isBranchingFrom" class="branch-badge-active">分支中</span>
          <button
            v-else
            class="continue-btn"
            @click="onContinue"
          >
            从此继续
          </button>
        </div>
      </div>
    </div>

    <div v-if="hasChildren" class="children">
      <VersionTreeNode
        v-for="(child, index) in orderedChildren"
        :key="child.id"
        :node="child"
        :active-id="activeId"
        :selected-id="selectedId"
        :parent-version-id="parentVersionId"
        :is-root="false"
        :is-last="index === orderedChildren.length - 1"
        @select="$emit('select', $event)"
        @continue="$emit('continue', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VersionTreeNode from './VersionTreeNode.vue'

const props = defineProps({
  node: { type: Object, required: true },
  activeId: { type: String, default: '' },
  selectedId: { type: String, default: '' },
  parentVersionId: { type: String, default: '' },
  isRoot: { type: Boolean, default: false },
  isLast: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'continue'])

function onContinue(e) {
  e.stopPropagation()
  emit('continue', props.node.id)
}

const isActive = computed(() => String(props.activeId) === String(props.node.id))
const isSelected = computed(() => String(props.selectedId) === String(props.node.id))
const isBranchingFrom = computed(() => String(props.parentVersionId) === String(props.node.id))
const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const summaryText = computed(() => {
  const text = props.node.summary || props.node.command || '新版本'
  return text.length > 28 ? `${text.slice(0, 28)}...` : text
})

function subtreeScore(node, activeId, selectedId) {
  if (!node) return 0
  let score = 0
  if (String(node.id) === String(activeId)) score = Math.max(score, 3)
  if (String(node.id) === String(selectedId)) score = Math.max(score, 2)
  const children = Array.isArray(node.children) ? node.children : []
  for (const child of children) {
    score = Math.max(score, subtreeScore(child, activeId, selectedId))
  }
  return score
}

const orderedChildren = computed(() => {
  const children = Array.isArray(props.node.children) ? [...props.node.children] : []
  return children
    .map((child, index) => ({
      child,
      index,
      score: subtreeScore(child, props.activeId, props.selectedId)
    }))
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score
      return a.index - b.index
    })
    .map(item => item.child)
})
</script>

<style scoped>
.tree-node {
  position: relative;
}

.node-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
  min-height: 68px;
}

.node-rail {
  position: relative;
  width: 28px;
  flex-shrink: 0;
}

.rail-vertical {
  position: absolute;
  left: 13px;
  top: -10px;
  bottom: -10px;
  width: 2px;
  background: linear-gradient(180deg, #374151, #475569);
}

.node-rail.last .rail-vertical {
  bottom: 34px;
}

.rail-horizontal {
  position: absolute;
  left: 13px;
  top: 33px;
  width: 16px;
  height: 2px;
  background: #475569;
}

.rail-dot {
  position: absolute;
  left: 8px;
  top: 28px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #64748b;
  box-shadow: 0 0 0 4px rgba(100, 116, 139, 0.12);
}

.rail-dot.active {
  background: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.18), 0 0 12px rgba(96, 165, 250, 0.45);
}

.node-rail.root .rail-dot {
  top: 20px;
}

.node-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border-radius: 16px;
  border: 1px solid #2f3b52;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0)),
    #1f2937;
  cursor: pointer;
  transition: all 0.18s ease;
}

.node-card:hover {
  background: #243245;
  border-color: rgba(96, 165, 250, 0.45);
  transform: translateY(-1px);
}

.node-card.active {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.18), rgba(59, 130, 246, 0.05)), #1d2d42;
  border-color: #3b82f6;
  box-shadow: 0 12px 28px rgba(30, 64, 175, 0.16);
}

.node-card.selected {
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.25);
}

.node-card.branching {
  border-color: #f59e0b;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
}

.node-main {
  min-width: 0;
  flex: 1;
}

.node-top {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.node-title {
  font-size: 14px;
  line-height: 1.4;
  color: #e5eefb;
  font-weight: 600;
}

.node-command {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-badge {
  flex-shrink: 0;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid transparent;
}

.root-badge {
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.22);
}

.branch-badge {
  color: #c4b5fd;
  background: rgba(139, 92, 246, 0.12);
  border-color: rgba(139, 92, 246, 0.18);
}

.leaf-badge {
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.08);
  border-color: rgba(148, 163, 184, 0.12);
}

.node-actions { flex-shrink: 0; position: relative; z-index: 1; }

.current-badge {
  font-size: 11px;
  font-weight: 600;
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.12);
  border: 1px solid rgba(96, 165, 250, 0.18);
  border-radius: 999px;
  padding: 5px 10px;
}

.branch-badge-active {
  font-size: 11px;
  font-weight: 600;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.22);
  border-radius: 999px;
  padding: 5px 10px;
}

.continue-btn {
  font-size: 11px;
  color: #dbeafe;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid #3b4a66;
  border-radius: 10px;
  padding: 6px 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.continue-btn:hover {
  background: rgba(30, 64, 175, 0.18);
  border-color: #60a5fa;
}

.children {
  margin-left: 26px;
  padding-left: 20px;
  position: relative;
}

.children::before {
  content: '';
  position: absolute;
  left: 33px;
  top: -10px;
  bottom: 20px;
  width: 2px;
  background: linear-gradient(180deg, rgba(71, 85, 105, 0.95), rgba(71, 85, 105, 0.18));
}
</style>

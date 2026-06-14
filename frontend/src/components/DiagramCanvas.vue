<template>
  <div class="diagram-wrapper" ref="wrapperRef">
    <div ref="containerRef" class="lf-container"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { LogicFlow } from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import '@logicflow/extension/dist/index.css'
import { registerCardNodes } from '../nodes/lfNodes.js'

const props = defineProps({
  diagram: { type: Object, default: null }
})

const containerRef = ref(null)
const wrapperRef = ref(null)
let lf = null

function getCanvasSize() {
  if (!wrapperRef.value) return { w: 1000, h: 600 }
  const rect = wrapperRef.value.getBoundingClientRect()
  return { w: Math.max(rect.width - 2, 600), h: Math.max(rect.height - 2, 400) }
}

// ==================== 布局引擎 ====================

function computeNodeSizes(nodes) {
  return nodes.map(n => {
    const label = n.label || ''
    const lines = label.split('\n')
    let maxLen = 0
    lines.forEach(l => { maxLen = Math.max(maxLen, l.length) })
    return { ...n, width: Math.max(maxLen * 12 + 32, 80), height: Math.max(lines.length * 18 + 20, 44), lines }
  })
}

function buildAdj(nodes, edges) {
  const adj = {}, radj = {}
  nodes.forEach(n => { adj[n.id] = []; radj[n.id] = [] })
  edges.forEach(e => {
    if (adj[e.source]) adj[e.source].push(e.target)
    if (radj[e.target]) radj[e.target].push(e.source)
  })
  return { adj, radj }
}

function layeredLayout(rawNodes, rawEdges, cw, ch) {
  const sizes = computeNodeSizes(rawNodes)
  const map = {}
  sizes.forEach(s => { map[s.id] = s })
  const { adj, radj } = buildAdj(rawNodes, rawEdges)
  const inDegree = {}
  rawNodes.forEach(n => { inDegree[n.id] = (radj[n.id] || []).length })
  const roots = rawNodes.filter(n => inDegree[n.id] === 0)
  if (roots.length === 0) roots.push(rawNodes[0])

  const layers = []
  const visited = new Set()
  let queue = roots.map(r => r.id)
  queue.forEach(id => visited.add(id))
  while (queue.length > 0) {
    layers.push([...queue])
    const next = []
    for (const id of queue) {
      for (const nb of (adj[id] || [])) {
        if (!visited.has(nb)) { visited.add(nb); next.push(nb) }
      }
    }
    queue = next
  }

  const positions = {}
  const GAP_Y = 80
  const maxHeights = layers.map(layer => Math.max(...layer.map(id => map[id]?.height || 44), 44))
  let yAcc = 50
  const layerYs = maxHeights.map(h => { const y = yAcc + h / 2; yAcc += h + GAP_Y; return y })

  const PAD_X = 30
  layers.forEach((layer, li) => {
    const totalW = layer.reduce((s, id) => s + (map[id]?.width || 80) + PAD_X, 0) - PAD_X
    let xCursor = (cw - totalW) / 2
    if (layer.length === 1) xCursor = cw / 2 - (map[layer[0]]?.width || 80) / 2
    layer.forEach(id => {
      const s = map[id]
      positions[id] = { x: xCursor + (s?.width || 80) / 2, y: layerYs[li] }
      xCursor += (s?.width || 80) + PAD_X
    })
  })
  return positions
}

function forceLayout(rawNodes, rawEdges, cw, ch) {
  const sizes = computeNodeSizes(rawNodes)
  const sizeMap = {}
  sizes.forEach(s => { sizeMap[s.id] = s })
  const positions = {}
  const cx = cw / 2, cy = ch / 2
  const count = sizes.length

  // 初始散布半径 — 按节点数量+大小动态计算，确保不重叠
  const avgW = sizes.reduce((s, n) => s + (n.width || 120), 0) / Math.max(count, 1)
  const avgH = sizes.reduce((s, n) => s + (n.height || 80), 0) / Math.max(count, 1)
  const spreadR = Math.max(Math.min(cw, ch) * 0.38, avgW * count * 0.25)

  sizes.forEach((s, i) => {
    const angle = (i / count) * 2 * Math.PI
    positions[s.id] = { x: cx + spreadR * Math.cos(angle), y: cy + spreadR * Math.sin(angle) }
  })

  const edgeSet = rawEdges.map(e => ({ source: e.source, target: e.target }))
  const iterations = 120, PAD = 60

  for (let iter = 0; iter < iterations; iter++) {
    const forces = {}
    rawNodes.forEach(n => { forces[n.id] = { fx: 0, fy: 0 } })
    // 节点间排斥力
    for (let i = 0; i < rawNodes.length; i++) {
      for (let j = i + 1; j < rawNodes.length; j++) {
        const a = rawNodes[i].id, b = rawNodes[j].id
        const sa = sizeMap[a], sb = sizeMap[b]
        if (!sa || !sb) continue
        const dx = positions[a].x - positions[b].x
        const dy = positions[a].y - positions[b].y
        const dist = Math.sqrt(dx * dx + dy * dy) || 1
        const minDist = Math.max(sa.width, sa.height) / 2 + Math.max(sb.width, sb.height) / 2 + PAD
        // 距离越近排斥力越大
        const repForce = dist < minDist ? 15000 / (dist * dist + 30) : 12000 / (dist * dist)
        forces[a].fx += (dx / dist) * repForce
        forces[a].fy += (dy / dist) * repForce
        forces[b].fx -= (dx / dist) * repForce
        forces[b].fy -= (dy / dist) * repForce
      }
    }
    // 边引力
    edgeSet.forEach(({ source, target }) => {
      if (!positions[source] || !positions[target]) return
      const dx = positions[target].x - positions[source].x
      const dy = positions[target].y - positions[source].y
      const dist = Math.sqrt(dx * dx + dy * dy) || 1
      const sa = sizeMap[source], sb = sizeMap[target]
      const ideal = Math.max(sa?.width || 80, sb?.width || 80) + 140
      const attForce = (dist - ideal) * 0.03
      forces[source].fx += (dx / dist) * attForce
      forces[source].fy += (dy / dist) * attForce
      forces[target].fx -= (dx / dist) * attForce
      forces[target].fy -= (dy / dist) * attForce
    })
    // 中心引力
    rawNodes.forEach(n => {
      forces[n.id].fx += (cx - positions[n.id].x) * 0.0008
      forces[n.id].fy += (cy - positions[n.id].y) * 0.0008
    })
    // 阻尼
    const damp = 0.85 * (1 - iter / iterations * 0.45)
    rawNodes.forEach(n => {
      const s = sizeMap[n.id]
      if (!s) return
      positions[n.id].x += forces[n.id].fx * damp
      positions[n.id].y += forces[n.id].fy * damp
      positions[n.id].x = Math.max(s.width / 2 + 20, Math.min(cw - s.width / 2 - 20, positions[n.id].x))
      positions[n.id].y = Math.max(s.height / 2 + 20, Math.min(ch - s.height / 2 - 20, positions[n.id].y))
    })
  }
  return positions
}

function gridLayout(rawNodes, cw, ch) {
  const sizes = computeNodeSizes(rawNodes)
  const positions = {}
  const maxW = Math.max(...sizes.map(s => s.width)) + 50
  const maxH = Math.max(...sizes.map(s => s.height)) + 50
  const n = sizes.length
  const cols = Math.max(2, Math.min(Math.ceil(Math.sqrt(n * 1.5)), n))
  const rows = Math.ceil(n / cols)
  const offsetX = (cw - cols * maxW) / 2 + maxW / 2
  const offsetY = Math.max(80, (ch - rows * maxH) / 2 + maxH / 2)
  sizes.forEach((s, i) => {
    positions[s.id] = { x: offsetX + (i % cols) * maxW, y: offsetY + Math.floor(i / cols) * maxH }
  })
  return positions
}

function radialLayout(rawNodes, rawEdges, cw, ch) {
  const positions = {}
  const { adj } = buildAdj(rawNodes, rawEdges)
  const rootId = rawNodes[0]?.id
  if (!rootId) return positions
  const depth = { [rootId]: 0 }
  const depthNodes = { 0: [rootId] }
  const queue = [rootId]
  while (queue.length > 0) {
    const id = queue.shift()
    for (const child of (adj[id] || [])) {
      if (depth[child] === undefined) {
        depth[child] = depth[id] + 1
        if (!depthNodes[depth[child]]) depthNodes[depth[child]] = []
        depthNodes[depth[child]].push(child)
        queue.push(child)
      }
    }
  }
  const cx = cw / 2, cy = ch / 2
  for (const d of Object.keys(depthNodes).map(Number)) {
    const ids = depthNodes[d]
    if (d === 0) { positions[ids[0]] = { x: cx, y: cy }; continue }
    const r = 90 + d * 130
    const span = Math.min(Math.PI * 2, ids.length * 0.6)
    ids.forEach((id, i) => {
      const angle = -Math.PI / 2 + (i / Math.max(ids.length - 1, 1)) * span
      positions[id] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
    })
  }
  return positions
}

// ==================== 干净浅色主题 ====================

const cleanTheme = {
  rect: {
    fill: '#ffffff',
    stroke: '#e2e8f0',
    strokeWidth: 1.5,
    radius: 12,
    width: 120,
    height: 48
  },
  ellipse: {
    fill: '#f8fafc',
    stroke: '#e2e8f0',
    strokeWidth: 1.5
  },
  diamond: {
    fill: '#fefce8',
    stroke: '#f59e0b',
    strokeWidth: 1.5
  },
  circle: {
    fill: '#ffffff',
    stroke: '#e2e8f0',
    strokeWidth: 2,
    r: 40
  },
  nodeText: {
    color: '#1e293b',
    fontSize: 12,
    overflowMode: 'autoWrap'
  },
  edgeText: {
    color: '#94a3b8',
    fontSize: 11,
    background: { fill: '#ffffff' }
  },
  polyline: { stroke: '#cbd5e1', strokeWidth: 1.5 },
  bezier: { stroke: '#cbd5e1', strokeWidth: 1.5 },
  line: { stroke: '#cbd5e1', strokeWidth: 1.5 },
  arrow: { offset: 8, verticalLength: 4 },
  outline: { stroke: '#6366f1', hover: { stroke: '#6366f1' } },
  anchor: { fill: '#6366f1', stroke: '#6366f1' }
}

// ==================== 数据转换 ====================

function toLogicFlowData(diagram, cw, ch) {
  if (!diagram || !diagram.nodes) return { nodes: [], edges: [] }
  const { nodes: rawNodes, edges: rawEdges, type } = diagram

  let positions = {}
  switch (type) {
    case 'flowchart':     positions = layeredLayout(rawNodes, rawEdges, cw, ch); break
    case 'mindmap':       positions = radialLayout(rawNodes, rawEdges, cw, ch); break
    case 'er':            positions = forceLayout(rawNodes, rawEdges, cw, ch); break
    case 'class':         positions = forceLayout(rawNodes, rawEdges, cw, ch); break
    case 'usecase':       positions = forceLayout(rawNodes, rawEdges, cw, ch); break
    case 'architecture':
    default:              positions = gridLayout(rawNodes, cw, ch); break
  }

  const sizes = computeNodeSizes(rawNodes)
  const sizeMap = {}
  sizes.forEach(s => { sizeMap[s.id] = s })

  const nodes = rawNodes.map(n => {
    const pos = positions[n.id] || {}
    const label = n.label || ''
    const s = sizeMap[n.id] || { width: 120, height: 48 }
    return {
      id: n.id,
      type: n.nodeType || 'process',
      x: pos.x ?? 100,
      y: pos.y ?? 100,
      text: { value: label, x: pos.x ?? 100, y: pos.y ?? 100 },
      properties: {
        label,
        width: Math.max(s.width, 100),
        height: Math.max(s.height, 44),
        fill: nodeFill(n.nodeType),
        stroke: nodeStroke(n.nodeType)
      }
    }
  })

  const edges = (rawEdges || []).map((e, i) => ({
    id: `e${i}`,
    type: 'polyline',
    sourceNodeId: e.source,
    targetNodeId: e.target,
    text: e.label || '',
    properties: {}
  }))

  return { nodes, edges }
}

function nodeFill(type) {
  const m = {
    start: '#f0fdf4', end: '#fef2f2', decision: '#fefce8',
    entity: '#ffffff',
    class: '#eff6ff', interface: '#fefce8', abstract: '#fef2f2',
    actor: '#f0fdf4', usecase: '#fff7ed'
  }
  return m[type] || '#ffffff'
}
function nodeStroke(type) {
  const m = {
    decision: '#f59e0b', start: '#22c55e', end: '#ef4444',
    entity: '#e2e8f0',
    class: '#3b82f6', interface: '#eab308', abstract: '#ef4444',
    actor: '#22c55e', usecase: '#f97316'
  }
  return m[type] || '#e2e8f0'
}

// ==================== 生命周期 ====================

function initLogicFlow() {
  if (!containerRef.value || lf) return
  const size = getCanvasSize()

  lf = new LogicFlow({
    container: containerRef.value,
    width: size.w,
    height: size.h,
    background: { backgroundColor: '#f8fafc' },
    grid: false,
    keyboard: { enabled: false }
  })

  lf.setTheme(cleanTheme)
  registerCardNodes(lf)
}

function renderDiagram() {
  if (!lf) { initLogicFlow(); if (!lf) return }
  const size = getCanvasSize()
  const data = toLogicFlowData(props.diagram, size.w, size.h)
  lf.render(data)
  try { lf.zoomToFit({ maxZoom: 1.5 }) } catch (_) {}
}

watch(() => props.diagram, (val) => {
  if (!val?.nodes?.length) return
  if (lf) { try { lf.destroy() } catch (_) {}; lf = null }
  nextTick(() => {
    initLogicFlow()
    nextTick(() => renderDiagram())
  })
})

onMounted(() => {
  nextTick(() => {
    if (props.diagram?.nodes?.length) {
      initLogicFlow()
      nextTick(() => renderDiagram())
    }
  })
})

onBeforeUnmount(() => {
  if (lf) { try { lf.destroy() } catch (_) {}; lf = null }
})
</script>

<style scoped>
.diagram-wrapper {
  flex: 1;
  min-height: 0;
  background: #f8fafc;
  border-radius: 0;
}
.lf-container {
  width: 100%;
  height: 100%;
}
</style>

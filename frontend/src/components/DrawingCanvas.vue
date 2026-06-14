<template>
  <div class="canvas-wrapper">
    <svg class="drawing-canvas" viewBox="0 0 1000 600">
      <rect width="1000" height="600" fill="#f8fafc" rx="0" />

      <!-- 渲染图形列表 -->
      <template v-for="(shape, index) in shapes" :key="index">
        <circle
          v-if="shape.type === 'circle'"
          :cx="shape.cx" :cy="shape.cy" :r="shape.r"
          :fill="shape.color" :fill-opacity="shape.opacity ?? 0.7"
          :stroke="shape.color" :stroke-width="shape.strokeWidth ?? 2"
        />
        <rect
          v-else-if="shape.type === 'rect'"
          :x="shape.x" :y="shape.y" :width="shape.width" :height="shape.height"
          :fill="shape.color" :fill-opacity="shape.opacity ?? 0.7" rx="6"
          :stroke="shape.color" :stroke-width="shape.strokeWidth ?? 2"
        />
        <polygon
          v-else-if="shape.type === 'triangle'"
          :points="trianglePoints(shape)"
          :fill="shape.color" :fill-opacity="shape.opacity ?? 0.7"
          :stroke="shape.color" :stroke-width="shape.strokeWidth ?? 2"
        />
        <line
          v-else-if="shape.type === 'line'"
          :x1="shape.x1" :y1="shape.y1" :x2="shape.x2" :y2="shape.y2"
          :stroke="shape.color" :stroke-width="shape.strokeWidth ?? 2.5" stroke-linecap="round"
        />
        <text
          v-else-if="shape.type === 'text'"
          :x="shape.x" :y="shape.y"
          :fill="shape.color" :font-size="shape.fontSize"
          text-anchor="middle" dominant-baseline="middle"
          font-family="inherit"
        >{{ shape.content }}</text>
      </template>
    </svg>
  </div>
</template>

<script setup>
defineProps({
  shapes: { type: Array, default: () => [] }
})

function trianglePoints(shape) {
  const { cx, cy, size } = shape
  const h = size * Math.sqrt(3) / 2
  return `${cx},${cy - h * 0.6} ${cx - size / 2},${cy + h * 0.4} ${cx + size / 2},${cy + h * 0.4}`
}
</script>

<style scoped>
.canvas-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  overflow: hidden;
}
.drawing-canvas {
  width: 100%;
  height: 100%;
}
</style>

<template>
  <div class="canvas-wrapper">
    <svg
      viewBox="0 0 1000 600"
      class="drawing-canvas"
    >
      <!-- 背景网格参考线 -->
      <defs>
        <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
          <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#2a2a4a" stroke-width="0.5"/>
        </pattern>
      </defs>
      <rect width="1000" height="600" fill="#0f0f23" />
      <rect width="1000" height="600" fill="url(#grid)" />

      <!-- 画布中央十字标记 -->
      <line x1="500" y1="290" x2="500" y2="310" stroke="#333" stroke-width="1"/>
      <line x1="490" y1="300" x2="510" y2="300" stroke="#333" stroke-width="1"/>

      <!-- 渲染图形列表 -->
      <template v-for="(shape, index) in shapes" :key="index">
        <!-- 圆形 -->
        <circle
          v-if="shape.type === 'circle'"
          :cx="shape.cx"
          :cy="shape.cy"
          :r="shape.r"
          :fill="shape.color"
          fill-opacity="0.6"
          :stroke="shape.color"
          stroke-width="2"
        />
        <!-- 矩形 -->
        <rect
          v-else-if="shape.type === 'rect'"
          :x="shape.x"
          :y="shape.y"
          :width="shape.width"
          :height="shape.height"
          :fill="shape.color"
          fill-opacity="0.6"
          :stroke="shape.color"
          stroke-width="2"
        />
        <!-- 三角形 -->
        <polygon
          v-else-if="shape.type === 'triangle'"
          :points="trianglePoints(shape)"
          :fill="shape.color"
          fill-opacity="0.6"
          :stroke="shape.color"
          stroke-width="2"
        />
        <!-- 线段 -->
        <line
          v-else-if="shape.type === 'line'"
          :x1="shape.x1"
          :y1="shape.y1"
          :x2="shape.x2"
          :y2="shape.y2"
          :stroke="shape.color"
          stroke-width="3"
        />
        <!-- 文本 -->
        <text
          v-else-if="shape.type === 'text'"
          :x="shape.x"
          :y="shape.y"
          :fill="shape.color"
          :font-size="shape.fontSize"
          text-anchor="middle"
          dominant-baseline="middle"
        >{{ shape.content }}</text>
      </template>
    </svg>
  </div>
</template>

<script setup>
defineProps({
  shapes: {
    type: Array,
    default: () => []
  }
})

function trianglePoints(shape) {
  const { cx, cy, size } = shape
  const h = size * Math.sqrt(3) / 2
  return `${cx},${cy - h * 0.6} ${cx - size / 2},${cy + h * 0.4} ${cx + size / 2},${cy + h * 0.4}`
}
</script>

<style scoped>
.canvas-wrapper {
  width: 100%;
  max-width: 1000px;
  border: 2px solid #3a3a5a;
  border-radius: 8px;
  overflow: hidden;
  background: #0f0f23;
}
.drawing-canvas {
  display: block;
  width: 100%;
  height: auto;
}
</style>

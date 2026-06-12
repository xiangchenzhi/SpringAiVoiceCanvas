<template>
  <div class="app-shell">
    <!-- ========== 主画布区 (80%) ========== -->
    <div class="canvas-area">
      <!-- 顶栏 -->
      <div class="topbar">
        <h1 class="app-title">DiagramGPT</h1>
        <div class="mode-tabs">
          <button :class="['mode-btn', { active: mode === 'shape' }]" @click="switchMode('shape')">
            <span class="mode-icon">✏️</span> 自由绘图
          </button>
          <button :class="['mode-btn', { active: mode === 'diagram' }]" @click="switchMode('diagram')">
            <span class="mode-icon">📊</span> 图表生成
          </button>
        </div>
      </div>

      <!-- 画布 -->
      <DrawingCanvas v-if="mode === 'shape'" :shapes="shapes" />
      <DiagramCanvas v-if="mode === 'diagram'" :diagram="diagram" />
    </div>

    <!-- ========== 右侧 AI 面板 (20%) ========== -->
    <div class="ai-panel">
      <!-- AI 执行过程 -->
      <ProcessLog :entries="processLog" />

      <!-- ChatGPT 风格输入区 -->
      <div class="chat-input-area">
        <textarea
          v-model="inputText"
          class="chat-textarea"
          :placeholder="mode === 'shape'
            ? '描述你想画的图形...'
            : '描述你想要的图表...'"
          rows="3"
          @keydown.enter.exact="submitText"
          :disabled="loading"
        ></textarea>
        <div class="chat-actions">
          <span class="char-hint">Enter 发送</span>
          <button class="send-btn" @click="submitText" :disabled="loading || !inputText.trim()">
            <span v-if="loading" class="spinner"></span>
            <span v-else>➤</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import DrawingCanvas from './components/DrawingCanvas.vue'
import DiagramCanvas from './components/DiagramCanvas.vue'
import ProcessLog from './components/ProcessLog.vue'
import { sendVoiceCommand } from './api/voiceApi.js'
import { sendDiagramCommand } from './api/diagramApi.js'

const mode = ref('shape')
const shapes = ref([])
const diagram = ref(null)
const processLog = reactive([])
const inputText = ref('')
const loading = ref(false)

function switchMode(m) {
  mode.value = m
  diagram.value = null
  shapes.value = []
  processLog.length = 0
}

function addLog(msg, type = 'info') {
  processLog.push({ msg, type, time: Date.now() })
}

function executeCommand(cmd) {
  switch (cmd.action) {
    case 'circle':
      shapes.value.push({
        type: 'circle',
        cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300,
        r: cmd.params.r ?? 50, color: cmd.params.color ?? 'black'
      })
      break
    case 'rect':
      shapes.value.push({
        type: 'rect',
        x: cmd.params.x ?? 450, y: cmd.params.y ?? 260,
        width: cmd.params.width ?? 100, height: cmd.params.height ?? 80,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'triangle':
      shapes.value.push({
        type: 'triangle',
        cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300,
        size: cmd.params.size ?? 60, color: cmd.params.color ?? 'black'
      })
      break
    case 'line':
      shapes.value.push({
        type: 'line',
        x1: cmd.params.x1 ?? 0, y1: cmd.params.y1 ?? 0,
        x2: cmd.params.x2 ?? 100, y2: cmd.params.y2 ?? 100,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'text':
      shapes.value.push({
        type: 'text',
        x: cmd.params.x ?? 500, y: cmd.params.y ?? 300,
        content: cmd.params.content ?? '', color: cmd.params.color ?? 'black',
        fontSize: cmd.params.fontSize ?? 24
      })
      break
    case 'clear': shapes.value = []; break
    case 'undo': shapes.value.pop(); break
  }
}

async function submitText() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  loading.value = true
  inputText.value = ''
  processLog.length = 0

  try {
    if (mode.value === 'shape') {
      addLog('正在理解绘图指令...', 'info')
      const data = await sendVoiceCommand(text)
      const commands = data.commands || []
      addLog(`解析出 ${commands.length} 个绘图操作`, 'success')
      commands.forEach((cmd, i) => {
        executeCommand(cmd)
        addLog(`${cmd.action}: ${JSON.stringify(cmd.params)}`, 'info')
      })
      addLog('渲染完成', 'success')
    } else {
      addLog('正在分析图表需求...', 'info')
      const data = await sendDiagramCommand(text)
      const d = data.diagram
      if (!d) {
        addLog('未能解析出图表结构', 'error')
        return
      }
      addLog(`识别为「${labelForType(d.type)}」`, 'success')
      const nodes = d.nodes || []
      const edges = d.edges || []
      addLog(`提取 ${nodes.length} 个节点`, 'info')
      nodes.forEach(n => addLog(`  ${n.label}`, 'node'))
      if (edges.length > 0) {
        addLog(`构建 ${edges.length} 条关系`, 'info')
      }
      addLog('自动布局中...', 'info')
      diagram.value = d
      await nextTick()
      addLog('渲染完成 ✓', 'success')
    }
  } catch (e) {
    console.error(e)
    addLog('解析失败，请重试', 'error')
  } finally {
    loading.value = false
  }
}

function labelForType(t) {
  const m = { flowchart: '流程图', mindmap: '思维导图', er: 'ER 图', architecture: '架构图' }
  return m[t] || t
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Inter', 'Microsoft YaHei', -apple-system, sans-serif;
  background: #111827;
  color: #e2e8f0;
  min-height: 100vh;
  overflow: hidden;
}

/* ====== Shell ====== */
.app-shell {
  display: flex;
  height: 100vh;
  width: 100vw;
}

/* ====== 画布区 80% ====== */
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #1e293b;
  background: #111827;
  height: 56px;
  flex-shrink: 0;
}
.app-title {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.5px;
}
.mode-tabs {
  display: flex;
  gap: 4px;
  background: #1e293b;
  border-radius: 10px;
  padding: 3px;
}
.mode-btn {
  padding: 6px 16px;
  font-size: 13px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 5px;
}
.mode-btn.active {
  background: #334155;
  color: #f1f5f9;
  font-weight: 600;
}
.mode-btn:hover:not(.active) { color: #cbd5e1; }
.mode-icon { font-size: 14px; }

/* ====== AI 面板 20% ====== */
.ai-panel {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #1e293b;
  background: #0f172a;
}

/* ChatGPT 风格输入 */
.chat-input-area {
  margin: 12px;
  background: #1e293b;
  border-radius: 14px;
  border: 1px solid #334155;
  transition: border-color 0.2s;
  flex-shrink: 0;
}
.chat-input-area:focus-within {
  border-color: #6366f1;
}
.chat-textarea {
  width: 100%;
  padding: 12px 14px 4px;
  border: none;
  background: transparent;
  color: #e2e8f0;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  line-height: 1.5;
}
.chat-textarea::placeholder { color: #64748b; }
.chat-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 4px 12px 10px;
  gap: 8px;
}
.char-hint {
  font-size: 11px;
  color: #475569;
}
.send-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #6366f1;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.send-btn:hover:not(:disabled) { background: #818cf8; }
.send-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.spinner {
  width: 14px; height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

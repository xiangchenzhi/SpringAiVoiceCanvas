<template>
  <div class="app-shell">
    <!-- ========== 侧边栏 ========== -->
    <Sidebar
      :conversations="conversationList"
      :activeId="conversationId"
      @select="loadConversation"
      @new-conversation="newConversation"
    />

    <!-- ========== 主画布区 ========== -->
    <div class="canvas-area">
      <div class="topbar">
        <h1 class="app-title">DiagramGPT</h1>
        <span class="intent-badge" v-if="activeType">{{ labelForType(activeType) }}</span>
        <span class="session-title" v-if="currentTitle">{{ currentTitle }}</span>
      </div>

      <DrawingCanvas v-if="activeType === 'shape'" :shapes="shapes" />
      <DiagramCanvas v-if="activeType === 'diagram'" :diagram="diagram" />
      <ImageCanvas v-if="activeType === 'image'" :imageUrl="imageUrl" :loading="loading" :loadingStep="loadingStep" />

      <div v-if="!activeType && !loading" class="empty-canvas">
        <div class="empty-icon">💬</div>
        <p>描述你想要什么，AI 会帮你完成</p>
        <p class="empty-hint">试试说：画一只赛博朋克猫 / 画一个请假审批流程 / 画一个蓝色圆形</p>
      </div>
    </div>

    <!-- ========== 右侧 AI 面板 ========== -->
    <div class="ai-panel">
      <ProcessLog :entries="processLog" />

      <div class="chat-input-area">
        <textarea
          v-model="inputText"
          class="chat-textarea"
          placeholder="描述你想要什么..."
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
import { ref, reactive, nextTick, onMounted } from 'vue'
import DrawingCanvas from './components/DrawingCanvas.vue'
import DiagramCanvas from './components/DiagramCanvas.vue'
import ImageCanvas from './components/ImageCanvas.vue'
import ProcessLog from './components/ProcessLog.vue'
import Sidebar from './components/Sidebar.vue'
import { sendIntent } from './api/intentApi.js'
import { fetchConversations, fetchConversation } from './api/conversationApi.js'

const activeType = ref('')
const shapes = ref([])
const diagram = ref(null)
const imageUrl = ref('')
const loadingStep = ref('')
const processLog = reactive([])
const inputText = ref('')
const loading = ref(false)
const conversationId = ref('')
const conversationList = ref([])
const currentTitle = ref('')

onMounted(() => refreshConversationList())

async function refreshConversationList() {
  try { conversationList.value = await fetchConversations() } catch (e) { /* ignore */ }
}

function addLog(msg, type = 'info') {
  processLog.push({ msg, type, time: Date.now() })
}

function executeCommand(cmd) {
  switch (cmd.action) {
    case 'circle':
      shapes.value.push({ type: 'circle', cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300, r: cmd.params.r ?? 50, color: cmd.params.color ?? 'black' })
      break
    case 'rect':
      shapes.value.push({ type: 'rect', x: cmd.params.x ?? 450, y: cmd.params.y ?? 260, width: cmd.params.width ?? 100, height: cmd.params.height ?? 80, color: cmd.params.color ?? 'black' })
      break
    case 'triangle':
      shapes.value.push({ type: 'triangle', cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300, size: cmd.params.size ?? 60, color: cmd.params.color ?? 'black' })
      break
    case 'line':
      shapes.value.push({ type: 'line', x1: cmd.params.x1 ?? 0, y1: cmd.params.y1 ?? 0, x2: cmd.params.x2 ?? 100, y2: cmd.params.y2 ?? 100, color: cmd.params.color ?? 'black' })
      break
    case 'text':
      shapes.value.push({ type: 'text', x: cmd.params.x ?? 500, y: cmd.params.y ?? 300, content: cmd.params.content ?? '', color: cmd.params.color ?? 'black', fontSize: cmd.params.fontSize ?? 24 })
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
    addLog('正在理解你的意图...', 'info')
    const data = await sendIntent(text, conversationId.value)

    if (data.error) {
      addLog(`错误: ${data.error}`, 'error')
      return
    }

    // 服务器返回的 conversationId（新会话时生成）
    if (data.conversationId) {
      conversationId.value = data.conversationId
      currentTitle.value = data.title || ''
    }

    const type = data.type
    addLog(`识别意图: ${labelForType(type)}`, 'success')
    activeType.value = type

    if (type === 'shape') {
      shapes.value = []
      const commands = data.commands || []
      addLog(`解析出 ${commands.length} 个绘图操作`, 'success')
      commands.forEach(cmd => {
        executeCommand(cmd)
        addLog(`${cmd.action}`, 'info')
      })
      addLog('渲染完成', 'success')
    } else if (type === 'diagram') {
      const d = data.diagram
      if (!d) { addLog('未能解析出图表结构', 'error'); return }
      addLog(`图表类型: ${labelForDiagram(d.type)}`, 'success')
      const nodes = d.nodes || []
      addLog(`提取 ${nodes.length} 个节点`, 'info')
      nodes.forEach(n => addLog(`  ${n.label}`, 'node'))
      if ((d.edges || []).length > 0) addLog(`构建 ${d.edges.length} 条关系`, 'info')
      addLog('自动布局中...', 'info')
      diagram.value = d
      await nextTick()
      addLog('渲染完成', 'success')
    } else if (type === 'image') {
      const enhanced = data.enhancedPrompt || ''
      addLog('正在优化提示词...', 'info')
      loadingStep.value = 'enhancing'
      addLog(`原始输入: ${text}`, 'info')
      addLog(`优化后提示词: ${enhanced}`, 'node')
      loadingStep.value = 'generating'
      addLog('正在生成图片...', 'info')
      imageUrl.value = data.imageUrl
      await nextTick()
      addLog('图片生成完成', 'success')
    }

    refreshConversationList()
  } catch (e) {
    console.error(e)
    addLog(`错误: ${e.message}`, 'error')
  } finally {
    loading.value = false
    loadingStep.value = ''
  }
}

function newConversation() {
  conversationId.value = ''
  currentTitle.value = ''
  activeType.value = ''
  shapes.value = []
  diagram.value = null
  imageUrl.value = ''
  processLog.length = 0
}

async function loadConversation(id) {
  try {
    const c = await fetchConversation(id)
    if (c.error) return
    processLog.length = 0
    conversationId.value = c.id
    currentTitle.value = c.title || ''

    if (!c.type) { activeType.value = ''; return }
    activeType.value = c.type

    if (c.type === 'image') {
      imageUrl.value = c.lastImageUrl || ''
    } else if (c.lastResult) {
      try {
        const data = JSON.parse(c.lastResult)
        if (c.type === 'diagram') {
          diagram.value = data
        } else if (c.type === 'shape') {
          shapes.value = []
          data.forEach(cmd => executeCommand(cmd))
        }
      } catch (e) { /* ignore */ }
    }
  } catch (e) {
    console.error(e)
  }
}

function labelForType(t) {
  const m = { shape: '自由绘图', diagram: '图表生成', image: 'AI 绘画' }
  return m[t] || t
}

function labelForDiagram(t) {
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
.app-shell { display: flex; height: 100vh; width: 100vw; }
.canvas-area {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
}
.topbar {
  display: flex; align-items: center; padding: 12px 24px;
  border-bottom: 1px solid #1e293b; background: #111827;
  height: 56px; flex-shrink: 0; gap: 12px;
}
.app-title {
  font-size: 22px; font-weight: 700;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.5px;
}
.intent-badge {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px;
  background: #1e293b; color: #94a3b8;
}
.session-title {
  font-size: 13px; color: #64748b; margin-left: 4px;
  max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.empty-canvas {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; background: #f8fafc; color: #94a3b8; gap: 8px;
}
.empty-icon { font-size: 48px; }
.empty-hint { font-size: 13px; max-width: 500px; text-align: center; line-height: 1.6; }
.ai-panel {
  width: 300px; min-width: 300px; height: 100vh;
  background: #1e293b; border-left: 1px solid #334155;
  display: flex; flex-direction: column; overflow: hidden;
}
.chat-input-area { padding: 12px; border-top: 1px solid #334155; flex-shrink: 0; }
.chat-textarea {
  width: 100%; padding: 12px; border-radius: 10px;
  border: 1px solid #475569; background: #0f172a; color: #e2e8f0;
  font-size: 14px; resize: none; outline: none;
  font-family: inherit; line-height: 1.5;
}
.chat-textarea:focus { border-color: #60a5fa; }
.chat-textarea::placeholder { color: #64748b; }
.chat-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.char-hint { font-size: 11px; color: #64748b; }
.send-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  color: #fff; font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: opacity 0.2s;
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.send-btn:hover:not(:disabled) { opacity: 0.85; }
.spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

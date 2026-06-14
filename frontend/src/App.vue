<template>
  <LoginView v-if="!loggedIn" @loggedIn="loggedIn = true" />
  <div class="app-shell" v-else>
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
        <span class="voice-indicator" v-if="voiceSupported" :class="'voice-' + voiceState.toLowerCase()">
          {{ voiceState === 'LISTENING' ? '🎤 正在监听' : voiceState === 'PROCESSING' ? '⏳ AI处理中' : '⏸ 已暂停监听' }}
        </span>
        <span class="intent-badge" v-if="activeType">{{ labelForType(activeType) }}</span>
        <span class="session-title" v-if="currentTitle">{{ currentTitle }}</span>
        <span v-if="parentVersionId" class="branch-badge">分支模式</span>
        <div class="topbar-spacer"></div>
        <button
          v-if="conversationId && versionTree.length > 0"
          class="version-btn"
          @click="showVersionPopover = true"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/></svg>
          版本历史
          <span class="version-btn-count">{{ versionTree.length }}</span>
        </button>
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
      <!-- 分支模式提示 -->
      <div class="branch-panel" v-if="parentVersionId">
        <div class="branch-panel-inner">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/></svg>
          <span>分支模式 — 下次输入从此版本创建新分支</span>
          <button class="vp-cancel-btn" @click="cancelBranch">取消</button>
        </div>
      </div>

      <!-- 执行日志 -->
      <ProcessLog :entries="processLog" />

      <div class="chat-input-area">
        <!-- 底部工具栏：语音按钮 + 文字输入切换 -->
        <div class="input-toolbar">
          <button
            class="voice-btn"
            :class="{ listening: voiceState === 'LISTENING' }"
            @click="toggleVoice"
            :disabled="loading || !voiceSupported || voiceState === 'PROCESSING'"
            :title="voiceSupported ? '语音输入' : '浏览器不支持语音识别'"
          >
            {{ voiceState === 'LISTENING' ? '🎙️ 监听中' : '🎤 语音' }}
          </button>
          <div class="toolbar-right">
            <span v-if="voiceState === 'LISTENING' && voiceText" class="voice-text-inline">{{ voiceText }}</span>
            <button class="text-toggle-btn" @click="showTextInput = !showTextInput" :title="showTextInput ? '收起文字输入' : '展开文字输入'">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              {{ showTextInput ? '收起' : '文字' }}
            </button>
          </div>
        </div>
        <template v-if="showTextInput">
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
          <div v-if="voiceState === 'LISTENING' && voiceText" class="voice-text">识别：{{ voiceText }}</div>
        </template>
      </div>
    </div>

    <!-- 版本历史悬浮弹窗 -->
    <VersionTree
      :visible="showVersionPopover"
      :tree="versionTree"
      :activeId="currentVersionId"
      :selectedId="selectedVersionId"
      @close="showVersionPopover = false"
      @select-node="selectVersion"
      @continue-from="onContinueFromVersion"
    />
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, watch } from 'vue'
import DrawingCanvas from './components/DrawingCanvas.vue'
import DiagramCanvas from './components/DiagramCanvas.vue'
import ImageCanvas from './components/ImageCanvas.vue'
import ProcessLog from './components/ProcessLog.vue'
import Sidebar from './components/Sidebar.vue'
import VersionTree from './components/VersionTree.vue'
import LoginView from './components/LoginView.vue'
import { XunfeiAsrClient } from './api/xunfeiAsrClient.js'
import { isLoggedIn } from './api/authApi.js'
import { sendIntent } from './api/intentApi.js'
import { fetchConversations, fetchConversation, fetchVersionTree, fetchVersionDetail, switchVersion } from './api/conversationApi.js'

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
const currentVersionId = ref('')
const parentVersionId = ref('')
const versionTree = ref([])
const selectedVersionId = ref('')
const showVersionPopover = ref(false)
const loggedIn = ref(isLoggedIn())
const showTextInput = ref(false)

// ====== 语音状态机（讯飞 ASR） ======
const voiceSupported = ref(true)
const VoiceState = { LISTENING: 'LISTENING', PROCESSING: 'PROCESSING', PAUSED: 'PAUSED' }
const voiceState = ref(VoiceState.PAUSED)
const voiceText = ref('')
let asr = null

function createAsr() {
  asr = new XunfeiAsrClient({
    onInterim: (text) => { voiceText.value = text },
    onFinal: (text) => {
      if (!text || !text.trim()) {
        // 空结果：没人说话/VAD误触发，重置状态并恢复监听
        pauseVoice()
        resumeVoice()
        return
      }
      inputText.value = text
      submitText()
    },
    onError: (msg) => {
      console.error('讯飞 ASR:', msg)
      voiceState.value = VoiceState.PAUSED
    },
    onStateChange: (s) => {
      if (s === 'listening') voiceState.value = VoiceState.LISTENING
      else if (s === 'processing') voiceState.value = VoiceState.PROCESSING
      else voiceState.value = VoiceState.PAUSED
    }
  })
}

function toggleVoice() {
  if (voiceState.value === VoiceState.PROCESSING) return
  if (voiceState.value === VoiceState.LISTENING) {
    pauseVoice()
  } else if (voiceState.value === VoiceState.PAUSED) {
    resumeVoice()
  }
}

function startVoice() {
  if (!asr) createAsr()
  asr.start()
}

function pauseVoice() {
  if (asr) asr.pause()
  voiceText.value = ''
}

function resumeVoice() {
  if (asr && !loading.value) asr.resume()
}

// 版本树弹窗 → 暂停/恢复
watch(showVersionPopover, (val) => {
  if (val) pauseVoice()
  else resumeVoice()
})

onMounted(() => {
  if (loggedIn.value) {
    refreshConversationList()
    startVoice()
  }
})

watch(loggedIn, (val) => {
  if (val) {
    refreshConversationList()
    startVoice()
  }
})

async function refreshConversationList() {
  try { conversationList.value = await fetchConversations() } catch (e) { /* ignore */ }
}

function addLog(msg, type = 'info') {
  processLog.push({ msg, type, time: Date.now() })
}

function executeCommand(cmd) {
  switch (cmd.action) {
    case 'circle':
      shapes.value.push({ type: 'circle', cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300, r: cmd.params.r ?? 50, color: cmd.params.color ?? 'black', opacity: cmd.params.opacity, strokeWidth: cmd.params.strokeWidth })
      break
    case 'rect':
      shapes.value.push({ type: 'rect', x: cmd.params.x ?? 450, y: cmd.params.y ?? 260, width: cmd.params.width ?? 100, height: cmd.params.height ?? 80, color: cmd.params.color ?? 'black', opacity: cmd.params.opacity, strokeWidth: cmd.params.strokeWidth })
      break
    case 'triangle':
      shapes.value.push({ type: 'triangle', cx: cmd.params.cx ?? 500, cy: cmd.params.cy ?? 300, size: cmd.params.size ?? 60, color: cmd.params.color ?? 'black', opacity: cmd.params.opacity, strokeWidth: cmd.params.strokeWidth })
      break
    case 'line':
      shapes.value.push({ type: 'line', x1: cmd.params.x1 ?? 0, y1: cmd.params.y1 ?? 0, x2: cmd.params.x2 ?? 100, y2: cmd.params.y2 ?? 100, color: cmd.params.color ?? 'black', strokeWidth: cmd.params.strokeWidth ?? 1, opacity: cmd.params.opacity })
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

  // 语音触发(PROCESSING)或手动输入(LISTENING)：暂停监听，AI完成后恢复
  const wasVoiceTriggered = voiceState.value === VoiceState.LISTENING || voiceState.value === VoiceState.PROCESSING
  if (wasVoiceTriggered) {
    pauseVoice()  // 必须设 asr._state='paused'，否则 finally 里 resume 不生效
    voiceState.value = VoiceState.PROCESSING
  }

  try {
    addLog('正在理解你的意图...', 'info')
    const data = await sendIntent(text, conversationId.value, parentVersionId.value)

    if (data.error) {
      addLog(`错误: ${data.error}`, 'error')
      voiceState.value = VoiceState.PAUSED
      return
    }

    // 使用服务器返回的 processLogs
    processLog.length = 0
    const logs = data.processLogs || []
    logs.forEach(l => addLog(l.content, l.level))

    if (data.conversationId) {
      conversationId.value = data.conversationId
      currentTitle.value = data.title || ''
    }

    const type = data.type
    activeType.value = type

    if (type === 'shape') {
      shapes.value = []
      const commands = data.commands || []
      commands.forEach(cmd => executeCommand(cmd))
    } else if (type === 'diagram') {
      const d = data.diagram
      if (!d) { addLog('未能解析出图表结构', 'error'); return }
      diagram.value = d
      await nextTick()
      addLog('渲染完成', 'success')
    } else if (type === 'image') {
      loadingStep.value = 'enhancing'
      await nextTick()
      imageUrl.value = data.imageUrl
      await nextTick()
    }

    if (data.versionId) {
      currentVersionId.value = data.versionId
    }

    // 分支模式：不清空，更新到新版本（继续沿分支走下去）
    // 正常模式：不设置 parentVersionId
    if (parentVersionId.value) {
      parentVersionId.value = data.versionId || parentVersionId.value
    }

    refreshConversationList()
    refreshVersionTree()
  } catch (e) {
    console.error(e)
    addLog(`错误: ${e.message}`, 'error')
  } finally {
    loading.value = false
    loadingStep.value = ''
    // AI 处理完成 → 恢复监听
    if (wasVoiceTriggered) {
      resumeVoice()
    }
  }
}

function newConversation() {
  pauseVoice()
  conversationId.value = ''
  currentTitle.value = ''
  currentVersionId.value = ''
  activeType.value = ''
  shapes.value = []
  diagram.value = null
  imageUrl.value = ''
  processLog.length = 0
  parentVersionId.value = ''
  versionTree.value = []
  selectedVersionId.value = ''
  resumeVoice()
}

async function loadConversation(id) {
  pauseVoice()
  try {
    const c = await fetchConversation(id)
    if (c.error) return
    processLog.length = 0
    conversationId.value = c.id
    currentTitle.value = c.title || ''
    currentVersionId.value = c.currentVersionId || ''
    parentVersionId.value = ''
    selectedVersionId.value = ''

    const convType = c.conversationType || c.lastType || ''
    if (!convType) { activeType.value = '' }
    else if (convType === 'IMAGE') {
      activeType.value = 'image'
      imageUrl.value = c.lastImageUrl || ''
    } else {
      if (c.diagramJson) {
        try {
          const data = JSON.parse(c.diagramJson)
          if (c.lastType === 'shape') {
            activeType.value = 'shape'
            shapes.value = []
            data.forEach(cmd => executeCommand(cmd))
          } else {
            activeType.value = 'diagram'
            diagram.value = data
          }
        } catch (e) {
          activeType.value = c.lastType || 'diagram'
        }
      } else {
        activeType.value = c.conversationType === 'IMAGE' ? 'image' : ''
      }
    }

    refreshVersionTree()
  } catch (e) {
    console.error(e)
  } finally {
    resumeVoice()
  }
}

async function refreshVersionTree() {
  if (!conversationId.value) return
  try {
    versionTree.value = await fetchVersionTree(conversationId.value)
  } catch (e) { /* ignore */ }
}

async function selectVersion(versionId) {
  pauseVoice()
  selectedVersionId.value = versionId
  parentVersionId.value = ''
  try {
    if (conversationId.value) {
      await switchVersion(conversationId.value, versionId)
      currentVersionId.value = versionId
    }
    const detail = await fetchVersionDetail(versionId)
    if (detail.logs) {
      processLog.length = 0
      detail.logs.forEach(l => addLog(l.content, l.level || 'info'))
    }
    if (detail.diagramJson) {
      try {
        const data = JSON.parse(detail.diagramJson)
        if (detail.diagramJson.startsWith('{"imageUrl"')) {
          activeType.value = 'image'
          imageUrl.value = data.imageUrl || ''
        } else if (Array.isArray(data)) {
          activeType.value = 'shape'
          shapes.value = []
          data.forEach(cmd => executeCommand(cmd))
        } else {
          activeType.value = 'diagram'
          diagram.value = data
        }
      } catch (e) { /* ignore */ }
    }
    await refreshVersionTree()
    refreshConversationList()
  } catch (e) { /* ignore */ }
  resumeVoice()
}

async function continueFromVersion(versionId) {
  parentVersionId.value = versionId
  selectedVersionId.value = versionId
  showVersionPopover.value = false
  // 从该版本恢复画布
  try {
    const detail = await fetchVersionDetail(versionId)
    if (detail.diagramJson) {
      try {
        const data = JSON.parse(detail.diagramJson)
        // image 类型特殊处理
        if (detail.diagramJson.startsWith('{"imageUrl"')) {
          if (activeType.value !== 'image') activeType.value = 'image'
          imageUrl.value = data.imageUrl || ''
        } else if (Array.isArray(data)) {
          activeType.value = 'shape'
          shapes.value = []
          data.forEach(cmd => executeCommand(cmd))
        } else {
          activeType.value = 'diagram'
          diagram.value = data
        }
      } catch (e) { /* ignore */ }
    }
    addLog('已切换到版本，下次输入将从此版本创建分支', 'info')
  } catch (e) { /* ignore */ }
}

function onContinueFromVersion(versionId) {
  continueFromVersion(versionId)
}

function cancelBranch() {
  pauseVoice()
  parentVersionId.value = ''
  selectedVersionId.value = ''
  resumeVoice()
}

function labelForType(t) {
  const m = { shape: '自由绘图', diagram: '图表生成', image: 'AI 绘画' }
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
.voice-indicator {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px;
  white-space: nowrap;
}
.voice-listening { background: #22c55e22; color: #22c55e; border: 1px solid #22c55e44; }
.voice-processing { background: #f59e0b22; color: #f59e0b; border: 1px solid #f59e0b44; }
.voice-paused { background: #64748b22; color: #64748b; border: 1px solid #64748b44; }
.intent-badge {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px;
  background: #1e293b; color: #94a3b8;
}
.session-title {
  font-size: 13px; color: #64748b; margin-left: 4px;
  max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.branch-badge {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px;
  background: #f59e0b22; color: #f59e0b; border: 1px solid #f59e0b44;
}
.topbar-spacer { flex: 1; }
.version-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 8px;
  border: 1px solid #334155; background: #1e293b;
  color: #cbd5e1; font-size: 13px; cursor: pointer;
  transition: all 0.15s;
}
.version-btn:hover { border-color: #60a5fa; color: #e2e8f0; background: #1e3050; }
.version-btn svg { opacity: 0.6; }
.version-btn:hover svg { opacity: 1; }
.version-btn-count {
  font-size: 11px; background: #334155; color: #94a3b8;
  padding: 1px 6px; border-radius: 8px; min-width: 18px; text-align: center;
}
.branch-panel {
  padding: 8px 12px; border-bottom: 1px solid #334155; flex-shrink: 0;
}
.branch-panel-inner {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 8px;
  background: #f59e0b11; border: 1px solid #f59e0b33;
  font-size: 12px; color: #f59e0b;
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
.vp-cancel-btn {
  font-size: 11px; padding: 2px 8px; border-radius: 4px;
  border: 1px solid #f59e0b44; background: transparent; color: #f59e0b;
  cursor: pointer;
}
.vp-cancel-btn:hover { background: #f59e0b22; }
.chat-input-area { padding: 10px 12px; border-top: 1px solid #334155; flex-shrink: 0; margin-top: auto; }
.input-toolbar {
  display: flex; align-items: center; justify-content: space-between;
}
.toolbar-right {
  display: flex; align-items: center; gap: 8px;
}
.voice-text-inline {
  font-size: 11px; color: #60a5fa; max-width: 120px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.text-toggle-btn {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 10px; border-radius: 8px;
  border: 1px solid #334155; background: #1e293b;
  color: #94a3b8; font-size: 12px; cursor: pointer;
  transition: all .2s;
}
.text-toggle-btn:hover { border-color: #60a5fa; color: #e2e8f0; }
.chat-textarea {
  width: 100%; padding: 12px; border-radius: 10px; margin-top: 10px;
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
.voice-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 10px; border-radius: 8px;
  border: 1px solid #475569; background: #1e293b;
  color: #e2e8f0; font-size: 12px; cursor: pointer;
  transition: all 0.2s;
}
.voice-btn:hover:not(:disabled) { border-color: #60a5fa; color: #60a5fa; }
.voice-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.voice-btn.listening { background: #e94560; border-color: #e94560; animation: pulse 1.2s infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.1); } }
.voice-hint { font-size: 12px; color: #f59e0b; margin-top: 6px; }
.voice-text { font-size: 12px; color: #60a5fa; margin-top: 4px; }
.spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

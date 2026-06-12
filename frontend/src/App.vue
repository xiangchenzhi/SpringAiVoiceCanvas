<template>
  <div class="app-container">
    <h1>AI 语音绘图工具</h1>
    <DrawingCanvas :shapes="shapes" />
    <VoiceButton
      :isListening="isListening"
      :transcript="transcript"
      @start="startListening"
      @stop="stopListening"
      @result="handleVoiceResult"
    />
    <CommandLog :commands="commandHistory" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DrawingCanvas from './components/DrawingCanvas.vue'
import VoiceButton from './components/VoiceButton.vue'
import CommandLog from './components/CommandLog.vue'
import { sendVoiceCommand } from './api/voiceApi.js'

const shapes = ref([])
const commandHistory = ref([])
const isListening = ref(false)
const transcript = ref('')

const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
let recognition = null

if (SpeechRecognition) {
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = false

  recognition.onresult = async (event) => {
    const text = event.results[0][0].transcript
    transcript.value = text
    isListening.value = false
    await handleVoiceResult(text)
  }

  recognition.onerror = () => {
    isListening.value = false
  }

  recognition.onend = () => {
    isListening.value = false
  }
}

function startListening() {
  if (!recognition) {
    alert('您的浏览器不支持语音识别，请使用 Chrome 浏览器')
    return
  }
  transcript.value = ''
  isListening.value = true
  recognition.start()
}

function stopListening() {
  recognition?.stop()
  isListening.value = false
}

function executeCommand(cmd) {
  switch (cmd.action) {
    case 'circle':
      shapes.value.push({
        type: 'circle',
        cx: cmd.params.cx ?? 500,
        cy: cmd.params.cy ?? 300,
        r: cmd.params.r ?? 50,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'rect':
      shapes.value.push({
        type: 'rect',
        x: cmd.params.x ?? 450,
        y: cmd.params.y ?? 260,
        width: cmd.params.width ?? 100,
        height: cmd.params.height ?? 80,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'triangle':
      shapes.value.push({
        type: 'triangle',
        cx: cmd.params.cx ?? 500,
        cy: cmd.params.cy ?? 300,
        size: cmd.params.size ?? 60,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'line':
      shapes.value.push({
        type: 'line',
        x1: cmd.params.x1 ?? 0,
        y1: cmd.params.y1 ?? 0,
        x2: cmd.params.x2 ?? 100,
        y2: cmd.params.y2 ?? 100,
        color: cmd.params.color ?? 'black'
      })
      break
    case 'text':
      shapes.value.push({
        type: 'text',
        x: cmd.params.x ?? 500,
        y: cmd.params.y ?? 300,
        content: cmd.params.content ?? '',
        color: cmd.params.color ?? 'black',
        fontSize: cmd.params.fontSize ?? 24
      })
      break
    case 'clear':
      shapes.value = []
      break
    case 'undo':
      shapes.value.pop()
      break
  }
}

async function handleVoiceResult(text) {
  try {
    const data = await sendVoiceCommand(text)
    const commands = data.commands || []
    commandHistory.value.push({
      text,
      commands,
      time: new Date().toLocaleTimeString()
    })
    commands.forEach(cmd => executeCommand(cmd))
  } catch (e) {
    console.error('命令解析失败:', e)
    alert('命令解析失败，请重试')
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Microsoft YaHei', sans-serif;
  background: #1a1a2e;
  color: #eee;
  min-height: 100vh;
}
.app-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
h1 {
  font-size: 24px;
  color: #00d4ff;
}
</style>

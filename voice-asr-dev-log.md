# 语音输入功能 — 问题与解决记录

## 技术架构

```
浏览器麦克风
  → getUserMedia (48kHz Mono)
  → AudioContext + ScriptProcessorNode 重采样到 16kHz PCM 16bit
  → WebSocket → SpringBoot /ws/asr
  → HMAC-SHA256 签名 → wss://iat.xf-yun.com
  → 返回结果 → WebSocket 回传前端
  → VAD 门控判断说完 → submitText()
```

## 遇到的问题 & 解决思路

---

### 1. 浏览器原生 SpeechRecognition 停顿过早发送

**现象：** 说半句停顿一下，浏览器 `continuous=false` + `isFinal=true` 直接触发发送。

**解决：** `continuous=true` + `onspeechend` + 2 秒静音倒计时。2 秒内再说话则重置，2 秒到才认为说完。

---

### 2. 未说话也自动循环识别

**现象：** `onFinal` 后自动 `start()`，静音触发 VAD → 空音频发讯飞 → 返回垃圾 → 再自动发送 → 死循环。

**解决：** 引入 `_speechHeard` 门控 + `SPEECH_THRESHOLD=0.02`。只有 RMS 超过阈值才标记 `_speechHeard=true`，从未说话则永远不触发 `_endUtterance`。

---

### 3. AudioContext 重复创建导致静音流

**现象：** 每次 `_endUtterance` 调 `audioCtx.close()`，下次 `start()` 新建 AudioContext。Chrome 对频繁创建有限制，后续实例拿到静音流，RMS 掉到 0.002。

**解决：** AudioContext 和 MediaStream 只创建一次，永不销毁。改用 `_attachProcessor` / `_detachProcessor` 切换。

---

### 4. 连接讯飞期间音频帧丢失

**现象：** `connectToXunfei()` 同步阻塞约 500ms，期间前端发的音频帧全被丢弃，讯飞只收到 1 帧，识别成 "apd"。

**解决：** 后端加 `audioBuffer`，连接中暂存音频帧，连接成功后一次性 flush。

---

### 5. 讯飞返回 403 The HTTP upgrade to WebSocket was denied

**原因：**
1. Java `URLEncoder.encode` 把空格编码成 `+`，讯飞期望 `%20`
2. `SimpleDateFormat` 未指定 `Locale.US`，中文系统上月份变成 "五月" 而非 "May"，导致签名算错

**解决：**
1. `encodeUrl()` 方法做 `.replace("+", "%20")`
2. `SimpleDateFormat` 加 `Locale.US`

---

### 6. wpgs 模式下 pgs 字段不能用

**现象：** 代码取 `pgs` 字段当结果，但讯飞 wpgs 模式下 `pgs` 是内部状态标识（`rpl`/`apd`），真实识别词在 `ws` 数组的 `cw[0].w` 里。

**解决：** 只用 `ws` 数组逐词拼接，忽略 `pgs`。

---

### 7. ws 数组是累积的，重复拼接

**现象：** 每帧的 `ws` 都包含"从头到现在"的全部词。后端每帧 `append` 导致 78 字 "你你好你好好你好好给..."。

**解决：** `ws` 模式下直接 `setLength(0).append(piece)` 替换而非追加。

---

### 8. final 帧 ws 被截短

**现象：** 最后一帧 `isFinal=true` 时 `ws` 只剩 `。`，`setLength(0).append("。")` 覆盖了累积的完整文本。

**解决：** final 帧时如果新 piece 比已有 buf 短，改为追加而非覆盖。

---

### 9. 识别完一直显示"AI处理中"不恢复

**现象：** ASR 的 `_endUtterance` 设 `_state='processing'`。`onFinal` 调 `submitText` 时 `voiceState` 已经是 `PROCESSING` 而非 `LISTENING`，`wasListening` 判断失败，`pauseVoice()` 不被调用 → `asr._state` 停在 `processing` → `resume()` 里 `_state === 'paused'` 永远 false → 死锁。

**解决：** `wasVoiceTriggered` 同时匹配 `LISTENING` 和 `PROCESSING`，并在 `submitText` 入口无条件 `pauseVoice()`，确保 `asr._state='paused'`，finally 里 `resume()` 才能正确重启。

---

## 状态机设计

```
页面加载 → LISTENING
  ↓ 说话...停顿2秒
_endUtterance → PROCESSING
  ↓ onFinal → submitText
  → pauseVoice (asr._state='paused')
  → voiceState = PROCESSING
  ↓ AI执行...
finally → resumeVoice → startVoice → LISTENING

打开版本树 → PAUSED
关闭版本树 → LISTENING
```

## 文件清单

| 文件 | 作用 |
|------|------|
| `backend/asr/XunfeiAsrHandler.java` | WS 中继、音频缓冲、wpgs 累积解码 |
| `backend/asr/XunfeiSignUtil.java` | HMAC-SHA256 签名 |
| `backend/config/WebSocketConfig.java` | `/ws/asr` 端点注册 |
| `frontend/api/xunfeiAsrClient.js` | AudioContext 重采样、VAD、WS 客户端 |
| `frontend/App.vue` | 语音状态机 LISTENING/PROCESSING/PAUSED |

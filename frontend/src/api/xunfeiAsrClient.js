/**
 * 讯飞语音识别客户端
 *
 * 麦克风 → AudioContext (永久持有) → ScriptProcessorNode → PCM 16kHz
 *   → WebSocket → SpringBoot → wss://iat.xf-yun.com
 *
 * AudioContext 和 MediaStream 只创建一次，_processor 可拆卸/重连。
 * VAD: AnalyserNode 监控，连续静音 N 毫秒后触发结束。
 */
export class XunfeiAsrClient {
  static SILENCE_GAP_MS = 2000
  static SPEECH_THRESHOLD = 0.03
  static TARGET_SAMPLE_RATE = 16000
  static WS_URL = 'ws://localhost:8080/ws/asr'
  static BUF_SIZE = 4096

  constructor(opts) {
    this._onInterim = opts.onInterim || (() => {})
    this._onFinal   = opts.onFinal   || (() => {})
    this._onError   = opts.onError   || (() => {})
    this._onStateChange = opts.onStateChange || (() => {})

    this._ws        = null
    this._stream    = null     // getUserMedia → 永驻
    this._audioCtx  = null     // AudioContext → 永驻
    this._srcNode   = null     // MediaStreamSource → 永驻
    this._analyser  = null     // AnalyserNode → 永驻
    this._processor = null     // ScriptProcessor → 可拆卸
    this._ratio     = 0

    this._silenceStart = null
    this._pending      = false
    this._speechHeard  = false
    this._frameCount   = 0
    this._lastRms      = 0
    this._state        = 'paused'
  }

  get state() { return this._state }

  _setState(s) {
    if (this._state !== s) { this._state = s; this._onStateChange(s) }
  }

  // ========== 启动 ==========

  async start() {
    // 如果正在监听，直接返回
    if (this._state === 'listening') return

    // 先设 listening 抢占状态，避免 async 空隙中被重复调用
    this._setState('listening')

    // ★ 核心修复：永远创建新 WS，不复用旧的。
    //    旧的可能已被后端关闭（close 信号还在网络上），复用就是定时炸弹。
    this._closeWS()

    try {
      await this._connectWS()
    } catch (e) {
      this._onError('连接失败，请检查后台服务是否启动')
      this._setState('paused')
      return
    }

    // 连接期间用户可能手动暂停了
    if (this._state !== 'listening') return

    // 首次：获取麦克风 + 创建 AudioContext（只做一次）
    if (!this._stream) {
      try {
        this._stream = await navigator.mediaDevices.getUserMedia({
          audio: { sampleRate: 48000, channelCount: 1, echoCancellation: true }
        })
      } catch (e) {
        this._onError('无法获取麦克风权限: ' + e.message)
        this._setState('paused')
        return
      }
    }

    if (!this._audioCtx) {
      this._audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 48000 })
      this._srcNode  = this._audioCtx.createMediaStreamSource(this._stream)
      this._analyser = this._audioCtx.createAnalyser()
      this._analyser.fftSize = 256
      this._srcNode.connect(this._analyser)
      this._ratio = XunfeiAsrClient.TARGET_SAMPLE_RATE / this._audioCtx.sampleRate
    }

    // ★ 关键：AudioContext 闲置一段时间后浏览器会自动挂起
    //    suspended 状态下 ScriptProcessorNode 的 onaudioprocess 不触发
    //    resume() 在非用户手势上下文中可能静默失败
    if (this._audioCtx.state !== 'running') {
      console.log('[ASR] AudioContext state:', this._audioCtx.state, '尝试恢复...')
      try { await this._audioCtx.resume() } catch (e) { /* ignore */ }
      // 如果 resume 失败，重建 AudioContext（新 ctx 在异步回调中可能处于 running）
      if (this._audioCtx.state !== 'running') {
        console.warn('[ASR] AudioContext 无法恢复，重建中...')
        this._audioCtx.close().catch(() => {})
        this._audioCtx = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 48000 })
        this._srcNode  = this._audioCtx.createMediaStreamSource(this._stream)
        this._analyser = this._audioCtx.createAnalyser()
        this._analyser.fftSize = 256
        this._srcNode.connect(this._analyser)
        this._ratio = XunfeiAsrClient.TARGET_SAMPLE_RATE / this._audioCtx.sampleRate
        console.log('[ASR] 新 AudioContext state:', this._audioCtx.state)
      }
    }

    // 挂载 processor（可多次）
    this._attachProcessor()

    this._silenceStart = null
    this._speechHeard  = false
    this._setState('listening')
  }

  _attachProcessor() {
    if (this._processor) return
    const p = this._audioCtx.createScriptProcessor(XunfeiAsrClient.BUF_SIZE, 1, 1)
    const ratio = this._ratio

    p.onaudioprocess = (event) => {
      if (this._state !== 'listening') return
      this._checkVAD()

      const input = event.inputBuffer.getChannelData(0)
      const len  = Math.floor(input.length * ratio)
      const resampled = new Float32Array(len)
      for (let i = 0; i < len; i++) {
        const idx = i / ratio
        const i0  = Math.floor(idx)
        const i1  = Math.min(i0 + 1, input.length - 1)
        resampled[i] = input[i0] * (1 - (idx - i0)) + input[i1] * (idx - i0)
      }

      const pcm = new Int16Array(len)
      for (let i = 0; i < len; i++) {
        pcm[i] = Math.max(-32768, Math.min(32767, resampled[i] * 32768))
      }

      if (this._ws && this._ws.readyState === WebSocket.OPEN) {
        this._frameCount++
        if (this._frameCount <= 3 || this._frameCount % 20 === 0) {
          console.log(`[ASR] 帧 #${this._frameCount} ${pcm.byteLength}B rms=${this._lastRms.toFixed(4)}`)
        }
        this._ws.send(pcm.buffer)
      }
    }

    this._srcNode.connect(p)
    p.connect(this._audioCtx.destination)
    this._processor = p
  }

  _detachProcessor() {
    if (this._processor) {
      this._processor.disconnect()
      this._processor.onaudioprocess = null
      this._processor = null
    }
  }

  // ========== VAD ==========

  _checkVAD() {
    if (!this._analyser) return
    const data = new Float32Array(this._analyser.fftSize)
    this._analyser.getFloatTimeDomainData(data)
    let sum = 0
    for (let i = 0; i < data.length; i++) sum += data[i] * data[i]
    const rms = Math.sqrt(sum / data.length)
    this._lastRms = rms

    if (rms >= XunfeiAsrClient.SPEECH_THRESHOLD) {
      this._speechHeard  = true
      this._silenceStart = null
      return
    }

    if (!this._speechHeard) return

    if (this._silenceStart === null) this._silenceStart = Date.now()
    if (Date.now() - this._silenceStart >= XunfeiAsrClient.SILENCE_GAP_MS && !this._pending) {
      this._endUtterance()
    }
  }

  _endUtterance() {
    this._pending = true
    this._setState('processing')

    // 发 end → 后端转发 status=2 给讯飞
    if (this._ws && this._ws.readyState === WebSocket.OPEN) {
      this._ws.send(JSON.stringify({ type: 'end' }))
    }
    // 断开 processor，但保留 AudioContext / 麦克风
    this._detachProcessor()
    this._silenceStart = null
  }

  // ========== WebSocket ==========

  _onWSMessage(event) {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'interim') {
        this._onInterim(msg.text)
      } else if (msg.type === 'final') {
        this._onFinal(msg.text)
        this._pending = false
        this._setState('paused')
      } else if (msg.type === 'error') {
        this._onError(msg.message || '识别错误')
        this._pending = false
        this._setState('paused')
      }
    } catch (e) { /* */ }
  }

  _connectWS() {
    return new Promise((resolve) => {
      this._ws = new WebSocket(XunfeiAsrClient.WS_URL)
      this._ws.binaryType = 'arraybuffer'
      this._ws.onopen    = () => resolve()
      this._ws.onmessage = (e) => this._onWSMessage(e)
      this._ws.onerror   = () => {
        if (this._state === 'listening') this._onError('WebSocket 连接异常')
      }
      this._ws.onclose   = () => {
        if (this._state === 'listening') {
          this._onError('WebSocket 断开')
          this._setState('paused')
        }
      }
    })
  }

  // ========== 暂停 / 恢复 / 停止 ==========

  _closeWS() {
    if (this._ws) {
      this._ws.onopen = null
      this._ws.onmessage = null
      this._ws.onerror = null
      this._ws.onclose = null
      try { this._ws.close() } catch (e) { /* */ }
      this._ws = null
    }
  }

  pause() {
    if (this._state === 'listening' || this._state === 'processing') {
      this._detachProcessor()
      this._pending = false
      this._closeWS()
      this._setState('paused')
    }
  }

  resume() {
    // processing 和 paused 都能恢复 —— processing 表示上次 VAD 结束后还没回到 paused
    if (this._state === 'paused' || this._state === 'processing') this.start()
  }

  /** 完全销毁（页面卸载） */
  async stop() {
    this._detachProcessor()
    if (this._audioCtx) { this._audioCtx.close().catch(() => {}); this._audioCtx = null; this._analyser = null; this._srcNode = null }
    if (this._stream)   { this._stream.getTracks().forEach(t => t.stop()); this._stream = null }
    this._closeWS()
    this._pending = false
    this._setState('paused')
  }
}

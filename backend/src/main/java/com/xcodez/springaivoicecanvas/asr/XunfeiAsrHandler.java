package com.xcodez.springaivoicecanvas.asr;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class XunfeiAsrHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(XunfeiAsrHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${xunfei.app-id:}")
    private String appId;

    @Value("${xunfei.api-key:}")
    private String apiKey;

    @Value("${xunfei.api-secret:}")
    private String apiSecret;

    /** 每个前端 session → 它的讯飞连接元数据 */
    private final Map<String, SessionState> states = new ConcurrentHashMap<>();

    // ========== 前端 WebSocket 事件 ==========

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("前端 ASR 连接: {}", session.getId());
        states.put(session.getId(), new SessionState(session));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            if (message instanceof TextMessage) {
                handleFrontendText(session, (TextMessage) message);
            } else if (message instanceof BinaryMessage) {
                handleFrontendBinary(session, (BinaryMessage) message);
            }
        } catch (Exception e) {
            log.error("处理前端消息失败", e);
        }
    }

    private void handleFrontendText(WebSocketSession session, TextMessage msg) {
        try {
            JsonNode node = mapper.readTree(msg.getPayload());
            String type = node.get("type").asText();
            SessionState state = states.get(session.getId());
            if (state == null) return;

            if ("end".equals(type)) {
                // 前端说完了 → 发结束帧 + 断开讯飞
                sendStatusFrame(state, 2);
                // 讯飞连接在收到最终结果后自己关
                state.ended = true;
            }
        } catch (Exception e) {
            log.error("解析前端文本消息失败", e);
        }
    }

    private void handleFrontendBinary(WebSocketSession session, BinaryMessage msg) {
        try {
            SessionState state = states.get(session.getId());
            if (state == null) return;

            byte[] audio = safeGetBytes(msg.getPayload());
            if (audio.length == 0) return;

            // 正在连接中 → 缓冲音频帧
            if (state.connecting) {
                state.audioBuffer.add(audio);
                return;
            }

            // 第一帧：连接讯飞 + 发首帧（带参数）
            if (state.xfSession == null || !state.xfSession.isOpen()) {
                state.connecting = true;
                state.audioBuffer.add(audio);
                connectToXunfei(state);
                // 连接成功后刷新缓冲帧
                flushAudioBuffer(state);
                state.connecting = false;
                return;
            }

            if (!state.ended) {
                int status = (state.seq.get() == 0) ? 0 : 1;
                sendAudioFrame(state, status, audio);
            }
        } catch (Exception e) {
            log.error("处理前端音频帧失败", e);
        }
    }

    private void flushAudioBuffer(SessionState state) {
        if (state.xfSession == null || !state.xfSession.isOpen()) return;
        for (byte[] audio : state.audioBuffer) {
            int status = (state.seq.get() == 0) ? 0 : 1;
            try { sendAudioFrame(state, status, audio); } catch (Exception e) { break; }
        }
        state.audioBuffer.clear();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable e) {
        log.error("前端 WS 传输错误: {}", session.getId(), e);
        cleanup(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("前端 ASR 断开: {}", session.getId());
        cleanup(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ========== 讯飞连接 ==========

    private void connectToXunfei(SessionState state) {
        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            log.error("讯飞密钥未配置: appId={}, apiKey={}, apiSecret={}",
                    appId != null && !appId.isBlank() ? "***" : "(空)",
                    apiKey != null && !apiKey.isBlank() ? "***" : "(空)",
                    apiSecret != null && !apiSecret.isBlank() ? "***" : "(空)");
            sendToFrontend(state, "{\"type\":\"error\",\"message\":\"讯飞密钥未配置，请设置 XF_APP_ID / XF_API_KEY / XF_API_SECRET 环境变量后重启\"}");
            return;
        }
        try {
            String url = XunfeiSignUtil.buildSignedUrl(apiKey, apiSecret);
            state.xfClient = new StandardWebSocketClient();
            state.xfSession = state.xfClient.execute(new XunfeiResultHandler(state), new WebSocketHttpHeaders(), URI.create(url)).get();
            state.seq.set(0);
            state.ended = false;
            state.resultBuf.setLength(0);
            log.info("已连接讯飞");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("连接讯飞被中断", e);
            sendToFrontend(state, "{\"type\":\"error\",\"message\":\"连接讯飞被中断\"}");
        } catch (Exception e) {
            log.error("连接讯飞失败", e);
            sendToFrontend(state, "{\"type\":\"error\",\"message\":\"连接讯飞失败\"}");
        }
    }

    private void sendAudioFrame(SessionState state, int status, byte[] audio) {
        try {
            int seq = state.seq.getAndIncrement();
            String audioB64 = Base64.getEncoder().encodeToString(audio);

            if (seq <= 2 || seq % 20 == 0) {
                log.info("发讯飞帧 seq={}, status={}, audioBytes={}", seq, status, audio.length);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"header\":{\"app_id\":\"").append(appId)
                    .append("\",\"status\":").append(status).append("}");

            if (status == 0) {
                // 首帧带参数
                sb.append(",\"parameter\":{\"iat\":{\"domain\":\"slm\",\"language\":\"zh_cn\",")
                        .append("\"accent\":\"mandarin\",\"eos\":6000,\"dwa\":\"wpgs\",")
                        .append("\"result\":{\"encoding\":\"utf8\",\"compress\":\"raw\",\"format\":\"json\"}}}}");
            }

            sb.append(",\"payload\":{\"audio\":{\"encoding\":\"raw\",\"sample_rate\":16000,")
                    .append("\"channels\":1,\"bit_depth\":16,\"seq\":").append(seq)
                    .append(",\"status\":").append(status)
                    .append(",\"audio\":\"").append(audioB64).append("\"}}}");

            state.xfSession.sendMessage(new TextMessage(sb.toString()));
        } catch (Exception e) {
            log.error("发送讯飞音频帧失败", e);
        }
    }

    private void sendStatusFrame(SessionState state, int status) {
        try {
            int seq = state.seq.getAndIncrement();
            log.info("发讯飞状态帧 seq={}, status={}", seq, status);
            String json = "{\"header\":{\"app_id\":\"" + appId + "\",\"status\":" + status + "}," +
                    "\"payload\":{\"audio\":{\"encoding\":\"raw\",\"sample_rate\":16000," +
                    "\"channels\":1,\"bit_depth\":16,\"seq\":" + seq +
                    ",\"status\":" + status + ",\"audio\":\"\"}}}";
            state.xfSession.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送讯飞状态帧失败", e);
        }
    }

    // ========== 讯飞结果处理器 ==========

    private class XunfeiResultHandler implements WebSocketHandler {
        private final SessionState state;

        XunfeiResultHandler(SessionState state) {
            this.state = state;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            // 握手成功，等待音频帧
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            try {
                String payload = ((TextMessage) message).getPayload();
                JsonNode root = mapper.readTree(payload);
                int code = root.path("header").path("code").asInt(-1);
                if (code != 0) {
                    log.warn("讯飞返回错误: {}", payload);
                    sendToFrontend(state, "{\"type\":\"error\",\"message\":\"讯飞返回错误\"}");
                    return;
                }

                JsonNode result = root.path("payload").path("result");
                if (result.isMissingNode()) return;

                String textB64 = result.path("text").asText();
                if (textB64.isEmpty()) return;

                byte[] decoded = Base64.getDecoder().decode(textB64);
                JsonNode textNode = mapper.readTree(decoded);

                // 从 ws 数组提取本次识别词
                StringBuilder fresh = new StringBuilder();
                if (textNode.has("ws")) {
                    for (JsonNode w : textNode.get("ws")) {
                        JsonNode cw = w.path("cw");
                        if (cw.isArray() && cw.size() > 0) {
                            fresh.append(cw.get(0).path("w").asText());
                        }
                    }
                }
                String piece = fresh.toString();
                boolean isFinal = result.path("status").asInt() == 2;

                // ws 在 wpgs 模式下是累积的，直接替换
                // 但 final 帧的 ws 可能已被截短，此时保留已有内容
                if (!piece.isEmpty()) {
                    if (isFinal && piece.length() < state.resultBuf.length()) {
                        // final 帧 ws 变短了，用新的追加到已有 buf 后面（追加 "." 等结尾字符）
                        state.resultBuf.append(piece);
                    } else {
                        state.resultBuf.setLength(0);
                        state.resultBuf.append(piece);
                    }
                }

                if (isFinal) {
                    String fullText = state.resultBuf.toString().trim();
                    log.info("讯飞累积结果: '{}' ({} 字)", fullText, fullText.length());
                    state.resultBuf.setLength(0); // 清空准备下一句
                    if (!fullText.isEmpty()) {
                        String json = mapper.writeValueAsString(Map.of(
                                "type", "final",
                                "text", fullText
                        ));
                        sendToFrontend(state, json);
                    }
                    try { session.close(); } catch (Exception ignored) {}
                } else {
                    // interim：只发给前端做 UI 展示提示，不触发业务
                    if (!piece.isEmpty()) {
                        String json = mapper.writeValueAsString(Map.of(
                                "type", "interim",
                                "text", state.resultBuf.toString()
                        ));
                        sendToFrontend(state, json);
                    }
                }
            } catch (Exception e) {
                log.error("解析讯飞结果失败", e);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable e) {
            log.error("讯飞 WS 传输错误", e);
            sendToFrontend(state, "{\"type\":\"error\",\"message\":\"讯飞连接异常\"}");
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            // 讯飞断开
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }

    // ========== 工具方法 ==========

    private void sendToFrontend(SessionState state, String json) {
        try {
            if (state.frontendSession.isOpen()) {
                state.frontendSession.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("向前端发送结果失败", e);
        }
    }

    /** ByteBuffer → byte[]：正确处理 position/limit */
    private byte[] safeGetBytes(java.nio.ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    private void cleanup(String sessionId) {
        SessionState state = states.remove(sessionId);
        if (state != null && state.xfSession != null) {
            try { state.xfSession.close(); } catch (Exception ignored) {}
        }
    }

    private static class SessionState {
        final WebSocketSession frontendSession;
        WebSocketSession xfSession;
        StandardWebSocketClient xfClient;
        AtomicInteger seq = new AtomicInteger(0);
        volatile boolean ended;
        volatile boolean connecting;
        java.util.List<byte[]> audioBuffer = new java.util.ArrayList<>();
        StringBuilder resultBuf = new StringBuilder();

        SessionState(WebSocketSession frontendSession) {
            this.frontendSession = frontendSession;
        }
    }
}

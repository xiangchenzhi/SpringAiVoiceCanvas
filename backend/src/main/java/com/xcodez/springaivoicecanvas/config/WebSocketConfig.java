package com.xcodez.springaivoicecanvas.config;

import com.xcodez.springaivoicecanvas.asr.XunfeiAsrHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final XunfeiAsrHandler xunfeiAsrHandler;

    public WebSocketConfig(XunfeiAsrHandler xunfeiAsrHandler) {
        this.xunfeiAsrHandler = xunfeiAsrHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(xunfeiAsrHandler, "/ws/asr")
                .setAllowedOrigins("*");
    }
}

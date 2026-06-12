package com.xcodez.springaivoicecanvas.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public WebClient siliconFlowClient(
            @Value("${siliconflow.image-url}") String imageUrl) {
        // imageUrl e.g. https://api.siliconflow.cn/v1/images/generations
        // 取其 base: https://api.siliconflow.cn
        String base = imageUrl;
        int thirdSlash = imageUrl.indexOf('/', imageUrl.indexOf("//") + 2);
        if (thirdSlash > 0) {
            base = imageUrl.substring(0, thirdSlash);
        }
        return WebClient.builder()
                .baseUrl(base)
                .build();
    }
}

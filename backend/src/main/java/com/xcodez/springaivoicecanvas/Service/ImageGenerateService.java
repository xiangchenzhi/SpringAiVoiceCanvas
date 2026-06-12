package com.xcodez.springaivoicecanvas.Service;

import com.xcodez.springaivoicecanvas.advisor.PromptEnhanceAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ImageGenerateService {

    private final ChatClient chatClient;
    private final PromptEnhanceAdvisor enhanceAdvisor;
    private final WebClient siliconFlowClient;
    private final String apiKey;

    private static final String MODEL = "Kwai-Kolors/Kolors";

    public ImageGenerateService(ChatClient chatClient,
                                PromptEnhanceAdvisor enhanceAdvisor,
                                WebClient siliconFlowClient,
                                @Value("${siliconflow.api-key}") String apiKey) {
        this.chatClient = chatClient;
        this.enhanceAdvisor = enhanceAdvisor;
        this.siliconFlowClient = siliconFlowClient;
        this.apiKey = apiKey;
    }

    /**
     * 完整的图片生成流程：
     * 1. DeepSeek 将用户中文描述扩写为高质量英文提示词
     * 2. FLUX 根据提示词生成图片
     */
    public ImageResult generate(String userPrompt) {
        // Step 1: 提示词扩写
        String enhancedPrompt = chatClient.prompt()
                .user(userPrompt)
                .advisors(enhanceAdvisor)
                .call()
                .content()
                .trim();

        // Step 2: 调用 FLUX 生成图片
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "prompt", enhancedPrompt
        );

        Map<?, ?> response = siliconFlowClient.post()
                .uri("/v1/images/generations")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String imageUrl = extractImageUrl(response);

        return new ImageResult(imageUrl, userPrompt, enhancedPrompt);
    }

    @SuppressWarnings("unchecked")
    private String extractImageUrl(Map<?, ?> response) {
        if (response == null) return null;
        Object imagesObj = response.get("images");
        if (imagesObj instanceof List && !((List<?>) imagesObj).isEmpty()) {
            Object first = ((List<?>) imagesObj).get(0);
            if (first instanceof Map) {
                Object url = ((Map<?, ?>) first).get("url");
                return url != null ? url.toString() : null;
            }
        }
        Object url = response.get("url");
        return url != null ? url.toString() : null;
    }

    public record ImageResult(String imageUrl, String originalPrompt, String enhancedPrompt) {}
}

package com.xcodez.springaivoicecanvas.controller;

import com.xcodez.springaivoicecanvas.Service.ImageGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageGenerateService imageService;

    public ImageController(ImageGenerateService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        if (prompt.isBlank()) {
            return Map.of("error", "prompt 不能为空");
        }

        try {
            ImageGenerateService.ImageResult result = imageService.generate(prompt, "");
            return Map.of(
                    "imageUrl", result.imageUrl() != null ? result.imageUrl() : "",
                    "originalPrompt", result.originalPrompt(),
                    "enhancedPrompt", result.enhancedPrompt()
            );
        } catch (Exception e) {
            log.error("图片生成失败: {}", e.getMessage(), e);
            String msg = e.getMessage();
            // 提取根因
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            return Map.of(
                    "error", cause.getMessage() != null ? cause.getMessage() : msg,
                    "imageUrl", "",
                    "originalPrompt", prompt,
                    "enhancedPrompt", ""
            );
        }
    }
}

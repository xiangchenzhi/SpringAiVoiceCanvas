package com.xcodez.springaivoicecanvas.controller;

import com.xcodez.springaivoicecanvas.model.Conversation;
import com.xcodez.springaivoicecanvas.model.Diagram;
import com.xcodez.springaivoicecanvas.model.ShapeCommand;
import com.xcodez.springaivoicecanvas.Service.AIService;
import com.xcodez.springaivoicecanvas.Service.ConversationService;
import com.xcodez.springaivoicecanvas.Service.ImageGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UnifiedController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedController.class);

    private final AIService aiService;
    private final ImageGenerateService imageService;
    private final ConversationService conversationService;

    public UnifiedController(AIService aiService,
                             ImageGenerateService imageService,
                             ConversationService conversationService) {
        this.aiService = aiService;
        this.imageService = imageService;
        this.conversationService = conversationService;
    }

    @PostMapping("/intent")
    public Map<String, Object> handleIntent(@RequestBody Map<String, String> body) {
        String transcript = body.getOrDefault("transcript", "").trim();
        String conversationId = body.getOrDefault("conversationId", "").trim();
        if (transcript.isBlank()) {
            return Map.of("error", "输入不能为空");
        }

        // 新会话：服务器生成 UUID，暂不设 type
        boolean isNew = conversationId.isBlank();
        if (isNew) {
            Conversation c = conversationService.create("");
            conversationId = c.getId();
        }

        // 意图分类（带记忆上下文）
        String intent;
        try {
            intent = aiService.classifyIntent(transcript, conversationId);
        } catch (Exception e) {
            log.error("意图分类失败，默认走 image", e);
            intent = "image";
        }

        var result = new HashMap<String, Object>();
        result.put("intent", intent);
        result.put("originalText", transcript);
        result.put("conversationId", conversationId);

        try {
            switch (intent) {
                case "diagram" -> {
                    Diagram diagram = aiService.parseDiagramCommand(transcript, conversationId);
                    result.put("type", "diagram");
                    result.put("diagram", diagram);
                    conversationService.updateResult(conversationId, "diagram", null, diagram);
                }
                case "shape" -> {
                    List<ShapeCommand> commands = aiService.parseVoiceCommand(transcript, conversationId);
                    result.put("type", "shape");
                    result.put("commands", commands);
                    conversationService.updateResult(conversationId, "shape", null, commands);
                }
                default -> {
                    ImageGenerateService.ImageResult ir = imageService.generate(transcript, conversationId);
                    result.put("type", "image");
                    result.put("imageUrl", ir.imageUrl());
                    result.put("enhancedPrompt", ir.enhancedPrompt());
                    conversationService.updateResult(conversationId, "image", ir.imageUrl(), null);
                }
            }
        } catch (Exception e) {
            log.error("意图执行失败: intent={}, msg={}", intent, e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        // 新会话：异步生成标题
        if (isNew) {
            conversationService.generateTitleAsync(conversationId, transcript);
            result.put("title", transcript); // 临时标题
        }

        return result;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> listConversations() {
        return conversationService.listAll().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle() != null ? c.getTitle() : "未命名会话");
            m.put("type", c.getType());
            m.put("lastImageUrl", c.getLastImageUrl());
            m.put("createdAt", c.getCreatedAt());
            m.put("updatedAt", c.getUpdatedAt());
            return m;
        }).toList();
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> getConversation(@PathVariable String id) {
        Conversation c = conversationService.getById(id);
        if (c == null) return Map.of("error", "会话不存在");
        return Map.of(
                "id", c.getId(),
                "title", c.getTitle() != null ? c.getTitle() : "未命名会话",
                "type", c.getType() != null ? c.getType() : "",
                "lastImageUrl", c.getLastImageUrl() != null ? c.getLastImageUrl() : "",
                "lastResult", c.getLastResult() != null ? c.getLastResult() : "",
                "createdAt", c.getCreatedAt()
        );
    }
}

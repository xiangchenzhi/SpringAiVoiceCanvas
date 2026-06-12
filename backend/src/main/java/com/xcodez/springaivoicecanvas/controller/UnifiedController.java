package com.xcodez.springaivoicecanvas.controller;

import com.xcodez.springaivoicecanvas.Service.AIService;
import com.xcodez.springaivoicecanvas.Service.ConversationService;
import com.xcodez.springaivoicecanvas.Service.DiagramVersionService;
import com.xcodez.springaivoicecanvas.Service.ImageGenerateService;
import com.xcodez.springaivoicecanvas.advisor.MemoryAdvisor;
import com.xcodez.springaivoicecanvas.memory.RedisChatMemoryService;
import com.xcodez.springaivoicecanvas.model.Conversation;
import com.xcodez.springaivoicecanvas.model.Diagram;
import com.xcodez.springaivoicecanvas.model.DiagramVersion;
import com.xcodez.springaivoicecanvas.model.ShapeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UnifiedController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedController.class);

    private final AIService aiService;
    private final ImageGenerateService imageService;
    private final ConversationService conversationService;
    private final DiagramVersionService versionService;
    private final RedisChatMemoryService memoryService;

    public UnifiedController(AIService aiService,
                             ImageGenerateService imageService,
                             ConversationService conversationService,
                             DiagramVersionService versionService,
                             RedisChatMemoryService memoryService) {
        this.aiService = aiService;
        this.imageService = imageService;
        this.conversationService = conversationService;
        this.versionService = versionService;
        this.memoryService = memoryService;
    }

    @PostMapping("/intent")
    public Map<String, Object> handleIntent(@RequestBody Map<String, String> body) {
        String transcript = body.getOrDefault("transcript", "").trim();
        String conversationId = body.getOrDefault("conversationId", "").trim();
        String parentVersionId = body.getOrDefault("parentVersionId", "").trim();
        if (transcript.isBlank()) {
            return Map.of("error", "输入不能为空");
        }

        boolean isNew = conversationId.isBlank();
        if (isNew) {
            Conversation c = conversationService.create("");
            conversationId = c.getId();
        }
        Conversation existingConv = !isNew ? conversationService.getById(conversationId) : null;

        // 变量提前声明，避免 try-finally 作用域问题
        String intent = "image";
        String imageUrl = null;
        String versionSummary = "";
        Object diagramObj = null;
        var result = new HashMap<String, Object>();
        List<Map<String, Object>> processLogs = new ArrayList<>();

        boolean useBranch = !parentVersionId.isBlank();
        String memoryBaseVersionId = useBranch
                ? parentVersionId
                : (existingConv != null ? existingConv.getCurrentVersionId() : null);
        String branchScratchKey = null;
        boolean useScratchMemory = memoryBaseVersionId != null && !memoryBaseVersionId.isBlank();
        if (useScratchMemory) {
            // 无论显式分支还是沿当前工作版本继续，都从版本快照复制到临时 scratch key，
            // 本轮对话只读写 scratch，完成后再固化到新版本，避免污染历史版本。
            branchScratchKey = "branch:" + UUID.randomUUID().toString();
            memoryService.copyMemory("version:" + memoryBaseVersionId, branchScratchKey);
            MemoryAdvisor.setBranchMemoryKey(branchScratchKey);
        }

        try {
            // 如果 conversationType 已确定，直接用它，不走 AI 分类
            String convType = existingConv != null ? existingConv.getConversationType() : null;
            if (convType != null && !convType.isBlank()) {
                // 已确定类型的会话，跳过 AI 分类，直接用存储的类型
                intent = "DIAGRAM".equals(convType) ? "diagram" : "image";
                log.info("会话 {} 类型已知={}，跳过 AI 意图分类", conversationId, convType);
            } else {
                try {
                    intent = aiService.classifyIntent(transcript, conversationId);
                } catch (Exception e) {
                    log.error("意图分类失败，默认走 image", e);
                    intent = "image";
                }
            }

            // 补充 conversationType
            if (isNew) {
                String newConvType = intent.equals("image") ? "IMAGE" : "DIAGRAM";
                conversationService.updateConversationType(conversationId, newConvType);
            }

            result.put("intent", intent);
            result.put("originalText", transcript);
            result.put("conversationId", conversationId);

            processLogs.add(Map.of("stepName", "意图分类", "content", "识别意图: " + intent, "level", "info"));

            try {
                switch (intent) {
                    case "diagram" -> {
                        Diagram diagram = aiService.parseDiagramCommand(transcript, conversationId);
                        result.put("type", "diagram");
                        result.put("diagram", diagram);
                        diagramObj = diagram;
                        versionSummary = "图表: " + (diagram.getType() != null ? diagram.getType() : "diagram");

                        processLogs.add(Map.of("stepName", "图表类型",
                                "content", "类型: " + diagram.getType(), "level", "success"));
                        processLogs.add(Map.of("stepName", "提取节点",
                                "content", "共 " + (diagram.getNodes() != null ? diagram.getNodes().size() : 0) + " 个节点", "level", "info"));

                        if (diagram.getNodes() != null) {
                            for (var n : diagram.getNodes()) {
                                processLogs.add(Map.of("stepName", "节点",
                                        "content", n.getLabel(), "level", "node"));
                            }
                        }
                        processLogs.add(Map.of("stepName", "自动布局",
                                "content", "已完成布局计算", "level", "success"));
                    }
                    case "shape" -> {
                        List<ShapeCommand> commands = aiService.parseVoiceCommand(transcript, conversationId);
                        result.put("type", "shape");
                        result.put("commands", commands);
                        diagramObj = commands;
                        versionSummary = "绘图: " + commands.size() + " 个操作";

                        processLogs.add(Map.of("stepName", "解析操作",
                                "content", "共 " + commands.size() + " 个绘图操作", "level", "success"));
                        commands.forEach(cmd -> processLogs.add(Map.of("stepName", "操作",
                                "content", cmd.getAction(), "level", "info")));
                    }
                    default -> {
                        ImageGenerateService.ImageResult ir = imageService.generate(transcript, conversationId);
                        result.put("type", "image");
                        result.put("imageUrl", ir.imageUrl());
                        result.put("enhancedPrompt", ir.enhancedPrompt());
                        imageUrl = ir.imageUrl();
                        versionSummary = "图片: " + (ir.enhancedPrompt() != null ?
                                ir.enhancedPrompt().substring(0, Math.min(30, ir.enhancedPrompt().length())) : transcript);

                        processLogs.add(Map.of("stepName", "优化提示词",
                                "content", "原始: " + transcript, "level", "info"));
                        processLogs.add(Map.of("stepName", "提示词优化结果",
                                "content", ir.enhancedPrompt(), "level", "node"));
                        processLogs.add(Map.of("stepName", "生成图片",
                                "content", "图片已生成", "level", "success"));
                    }
                }
            } catch (Exception e) {
                log.error("意图执行失败: intent={}, msg={}", intent, e.getMessage(), e);
                result.put("error", e.getMessage());
            }

            // 创建版本快照
            if (!intent.equals("image") || imageUrl != null) {
                // 分支模式：parent = 用户指定的版本；正常模式：parent = 当前工作版本
                Conversation currentConversation = conversationService.getById(conversationId);
                String parentId;
                if (!parentVersionId.isBlank()) {
                    parentId = parentVersionId;
                } else {
                    parentId = (currentConversation != null) ? currentConversation.getCurrentVersionId() : null;
                }

                DiagramVersion version = versionService.createVersion(
                        conversationId, parentId, transcript, versionSummary,
                        diagramObj != null ? diagramObj : Map.of("imageUrl", imageUrl != null ? imageUrl : ""),
                        processLogs
                );

                result.put("versionId", version.getId());

                if (isNew || conversationService.getById(conversationId).getRootVersionId() == null) {
                    conversationService.setRootVersion(conversationId, version.getId());
                }
                conversationService.switchToVersion(conversationId, version.getId());

                if (useScratchMemory) {
                    // scratch key 已包含基线版本记忆 + 本轮交换，直接移到新版本 key
                    memoryService.moveMemory(branchScratchKey, "version:" + version.getId());
                } else {
                    // 首轮对话：仍然从会话级热记忆快照到首个版本
                    memoryService.copyMemory(conversationId, "version:" + version.getId());
                }
            }
        } finally {
            MemoryAdvisor.clearBranchMemory();
        }
        conversationService.updateMeta(conversationId, intent, imageUrl);

        if (isNew) {
            conversationService.generateTitleAsync(conversationId, transcript);
            result.put("title", transcript);
        }

        result.put("processLogs", processLogs);
        return result;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> listConversations() {
        return conversationService.listAll().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle() != null ? c.getTitle() : "未命名会话");
            m.put("conversationType", c.getConversationType());
            m.put("lastType", c.getLastType());
            m.put("lastImageUrl", c.getLastImageUrl());
            m.put("currentVersionId", c.getCurrentVersionId());
            m.put("createdAt", c.getCreatedAt());
            m.put("updatedAt", c.getUpdatedAt());
            return m;
        }).toList();
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> getConversation(@PathVariable String id) {
        Conversation c = conversationService.getById(id);
        if (c == null) return Map.of("error", "会话不存在");

        DiagramVersion v = c.getCurrentVersionId() != null ?
                versionService.getVersion(c.getCurrentVersionId()) : null;

        return Map.of(
                "id", c.getId(),
                "title", c.getTitle() != null ? c.getTitle() : "未命名会话",
                "conversationType", c.getConversationType() != null ? c.getConversationType() : "",
                "lastType", c.getLastType() != null ? c.getLastType() : "",
                "lastImageUrl", c.getLastImageUrl() != null ? c.getLastImageUrl() : "",
                "currentVersionId", c.getCurrentVersionId(),
                "diagramJson", v != null ? v.getDiagramJson() : "",
                "createdAt", c.getCreatedAt()
        );
    }

    @GetMapping("/versions/{conversationId}/tree")
    public List<Map<String, Object>> getVersionTree(@PathVariable String conversationId) {
        return versionService.getVersionTree(conversationId);
    }

    @GetMapping("/versions/{versionId}")
    public Map<String, Object> getVersionDetail(@PathVariable String versionId) {
        return versionService.getVersionDetail(versionId);
    }

    @PostMapping("/versions/switch")
    public Map<String, Object> switchVersion(@RequestBody Map<String, String> body) {
        String conversationId = body.get("conversationId");
        String versionId = body.get("versionId");
        if (conversationId == null || versionId == null) {
            return Map.of("error", "缺少参数");
        }
        conversationService.switchToVersion(conversationId, versionId);

        DiagramVersion v = versionService.getVersion(versionId);
        return Map.of(
                "success", true,
                "versionId", versionId,
                "diagramJson", v != null ? v.getDiagramJson() : ""
        );
    }
}

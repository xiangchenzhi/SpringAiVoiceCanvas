package com.xcodez.springaivoicecanvas.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcodez.springaivoicecanvas.advisor.DiagramAdvisor;
import com.xcodez.springaivoicecanvas.advisor.DrawingCommandAdvisor;
import com.xcodez.springaivoicecanvas.model.Diagram;
import com.xcodez.springaivoicecanvas.model.ShapeCommand;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final DrawingCommandAdvisor commandAdvisor;
    private final DiagramAdvisor diagramAdvisor;
    private final ObjectMapper objectMapper;

    public AIService(ChatClient chatClient,
                     DrawingCommandAdvisor commandAdvisor,
                     DiagramAdvisor diagramAdvisor,
                     ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.commandAdvisor = commandAdvisor;
        this.diagramAdvisor = diagramAdvisor;
        this.objectMapper = objectMapper;
    }

    /**
     * Phase 1: 将用户的自然语言指令转换为结构化的绘图命令列表
     */
    public List<ShapeCommand> parseVoiceCommand(String userMessage) {
        String rawResponse = chatClient.prompt()
                .user(userMessage)
                .advisors(commandAdvisor)
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();

        String json = extractJson(rawResponse);
        try {
            return objectMapper.readValue(json, new TypeReference<List<ShapeCommand>>() {});
        } catch (Exception e) {
            throw new RuntimeException("模型返回的 JSON 解析失败: " + json, e);
        }
    }

    /**
     * Phase 2: 将用户的自然语言指令转换为图表结构（流程图/思维导图/ER图/架构图）
     */
    public Diagram parseDiagramCommand(String userMessage) {
        String rawResponse = chatClient.prompt()
                .user(userMessage)
                .advisors(diagramAdvisor)
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();

        String json = extractJson(rawResponse);
        try {
            return objectMapper.readValue(json, Diagram.class);
        } catch (Exception e) {
            throw new RuntimeException("模型返回的 Diagram JSON 解析失败: " + json, e);
        }
    }

    /**
     * 清洗模型返回文本，提取纯 JSON，处理可能的 ```json ... ``` 包裹
     */
    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            if (start == -1) {
                trimmed = trimmed.substring(3);
            } else {
                trimmed = trimmed.substring(start + 1);
            }
            int end = trimmed.lastIndexOf("```");
            if (end != -1) {
                trimmed = trimmed.substring(0, end);
            }
        }
        return trimmed.trim();
    }
}

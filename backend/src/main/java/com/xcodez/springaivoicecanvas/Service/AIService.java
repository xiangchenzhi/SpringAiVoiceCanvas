package com.xcodez.springaivoicecanvas.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcodez.springaivoicecanvas.advisor.DiagramAdvisor;
import com.xcodez.springaivoicecanvas.advisor.DrawingCommandAdvisor;
import com.xcodez.springaivoicecanvas.advisor.IntentAdvisor;
import com.xcodez.springaivoicecanvas.advisor.MemoryAdvisor;
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
    private final IntentAdvisor intentAdvisor;
    private final MemoryAdvisor memoryAdvisor;
    private final ObjectMapper objectMapper;

    public AIService(ChatClient chatClient,
                     DrawingCommandAdvisor commandAdvisor,
                     DiagramAdvisor diagramAdvisor,
                     IntentAdvisor intentAdvisor,
                     MemoryAdvisor memoryAdvisor,
                     ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.commandAdvisor = commandAdvisor;
        this.diagramAdvisor = diagramAdvisor;
        this.intentAdvisor = intentAdvisor;
        this.memoryAdvisor = memoryAdvisor;
        this.objectMapper = objectMapper;
    }

    public String classifyIntent(String userMessage, String conversationId) {
        String result;
        try {
            MemoryAdvisor.setConversationId(conversationId);
            result = chatClient.prompt()
                    .user(userMessage)
                    .advisors(intentAdvisor, memoryAdvisor)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();
        } finally {
            MemoryAdvisor.clearConversationId();
        }
        return result;
    }

    public List<ShapeCommand> parseVoiceCommand(String userMessage, String conversationId) {
        String rawResponse;
        try {
            MemoryAdvisor.setConversationId(conversationId);
            rawResponse = chatClient.prompt()
                    .user(userMessage)
                    .advisors(commandAdvisor, memoryAdvisor)
                    .call()
                    .chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        } finally {
            MemoryAdvisor.clearConversationId();
        }

        String json = extractJson(rawResponse);
        try {
            return objectMapper.readValue(json, new TypeReference<List<ShapeCommand>>() {});
        } catch (Exception e) {
            throw new RuntimeException("模型返回的 JSON 解析失败: " + json, e);
        }
    }

    public Diagram parseDiagramCommand(String userMessage, String conversationId) {
        String rawResponse;
        try {
            MemoryAdvisor.setConversationId(conversationId);
            rawResponse = chatClient.prompt()
                    .user(userMessage)
                    .advisors(diagramAdvisor, memoryAdvisor)
                    .call()
                    .chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        } finally {
            MemoryAdvisor.clearConversationId();
        }

        String json = extractJson(rawResponse);
        try {
            return objectMapper.readValue(json, Diagram.class);
        } catch (Exception e) {
            throw new RuntimeException("模型返回的 Diagram JSON 解析失败: " + json, e);
        }
    }

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

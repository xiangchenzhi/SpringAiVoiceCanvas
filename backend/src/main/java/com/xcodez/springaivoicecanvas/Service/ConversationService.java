package com.xcodez.springaivoicecanvas.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcodez.springaivoicecanvas.model.Conversation;
import com.xcodez.springaivoicecanvas.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository repo;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository repo, ChatClient chatClient, ObjectMapper objectMapper) {
        this.repo = repo;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public Conversation create(String type) {
        Conversation c = new Conversation(UUID.randomUUID().toString(), type);
        return repo.save(c);
    }

    public List<Conversation> listAll() {
        return repo.findAll().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .toList();
    }

    public Conversation getById(String id) {
        return repo.findById(id).orElse(null);
    }

    public void updateResult(String id, String type, String imageUrl, Object resultObj) {
        repo.findById(id).ifPresent(c -> {
            c.setType(type);
            c.setLastImageUrl(imageUrl);
            try {
                c.setLastResult(resultObj != null ? objectMapper.writeValueAsString(resultObj) : null);
            } catch (Exception ignored) {}
            c.setUpdatedAt(LocalDateTime.now());
            repo.save(c);
        });
    }

    @Async
    public void generateTitleAsync(String conversationId, String firstUserMessage) {
        try {
            String title = chatClient.prompt()
                    .user("用户的第一句话是：「" + firstUserMessage + "」。请为这个对话生成一个简短的中文标题（8个字以内），只返回标题，不要其他内容。")
                    .call()
                    .content()
                    .trim()
                    .replaceAll("^[\"「]|[\"」]$", "");

            repo.findById(conversationId).ifPresent(c -> {
                c.setTitle(title);
                repo.save(c);
            });
        } catch (Exception e) {
            log.warn("生成会话标题失败: {}", e.getMessage());
        }
    }
}

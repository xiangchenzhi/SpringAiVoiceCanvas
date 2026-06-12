package com.xcodez.springaivoicecanvas.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcodez.springaivoicecanvas.model.HistoryMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisChatMemoryService {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${chat.memory.max-rounds:5}")
    private int maxRounds;

    public RedisChatMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    public List<HistoryMessage> getHistory(String conversationId) {
        String json = redisTemplate.opsForValue().get(buildKey(conversationId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HistoryMessage>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public void saveExchange(String conversationId, String userText, String assistantText) {
        List<HistoryMessage> history = new ArrayList<>(getHistory(conversationId));
        history.add(new HistoryMessage("user", userText));
        history.add(new HistoryMessage("assistant", assistantText));

        while (history.size() > maxRounds * 2) {
            history.remove(0);
            history.remove(0);
        }

        try {
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(buildKey(conversationId), json);
        } catch (Exception ignored) {
        }
    }

    public void clearHistory(String conversationId) {
        redisTemplate.delete(buildKey(conversationId));
    }
}

package com.xcodez.springaivoicecanvas.Service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.xcodez.springaivoicecanvas.model.Conversation;
import com.xcodez.springaivoicecanvas.repository.ConversationRepository;
import com.xcodez.springaivoicecanvas.repository.DiagramVersionRepository;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository repo;
    private final DiagramVersionRepository versionRepo;
    private final ChatClient chatClient;

    public ConversationService(ConversationRepository repo, DiagramVersionRepository versionRepo, ChatClient chatClient) {
        this.repo = repo;
        this.versionRepo = versionRepo;
        this.chatClient = chatClient;
    }

    public Conversation create(String conversationType) {
        Conversation c = new Conversation(UUID.randomUUID().toString(), conversationType);
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

    public void setRootVersion(String conversationId, String versionId) {
        safeSave(conversationId, "setRootVersion", c -> {
            c.setRootVersionId(versionId);
            c.setCurrentVersionId(versionId);
        });
    }

    public void updateConversationType(String conversationId, String conversationType) {
        safeSave(conversationId, "updateConversationType", c -> {
            c.setConversationType(conversationType);
        });
    }

    public void switchToVersion(String conversationId, String versionId) {
        safeSave(conversationId, "switchToVersion", c -> {
            c.setCurrentVersionId(versionId);
        });
    }

    public void updateMeta(String conversationId, String lastType, String imageUrl) {
        safeSave(conversationId, "updateMeta", c -> {
            if (lastType != null) c.setLastType(lastType.substring(0, Math.min(lastType.length(), 48)));
            if (imageUrl != null) c.setLastImageUrl(imageUrl);
        });
    }

    public void delete(String conversationId) {
        versionRepo.deleteByConversationId(conversationId);
        repo.deleteById(conversationId);
    }

    /**
     * 安全保存：失败时记录日志但不抛异常，确保其他流程不受影响
     */
    private void safeSave(String conversationId, String op, java.util.function.Consumer<Conversation> modifier) {
        try {
            repo.findById(conversationId).ifPresent(c -> {
                modifier.accept(c);
                repo.save(c);
            });
        } catch (Exception e) {
            log.warn("Conversation {} 操作失败 (列宽不匹配? 请手动删表重建): {}", op, e.getMessage());
        }
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

            safeSave(conversationId, "generateTitle", c -> c.setTitle(title));
        } catch (Exception e) {
            log.warn("生成会话标题失败: {}", e.getMessage());
        }
    }
}

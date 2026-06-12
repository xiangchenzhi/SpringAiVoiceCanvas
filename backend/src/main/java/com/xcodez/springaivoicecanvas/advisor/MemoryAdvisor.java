package com.xcodez.springaivoicecanvas.advisor;

import com.xcodez.springaivoicecanvas.memory.RedisChatMemoryService;
import com.xcodez.springaivoicecanvas.model.HistoryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoryAdvisor implements CallAdvisor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MemoryAdvisor.class);

    private static final ThreadLocal<String> CONVERSATION_ID_HOLDER = new ThreadLocal<>();

    private final RedisChatMemoryService memoryService;

    public MemoryAdvisor(RedisChatMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 在调用前设置当前线程的会话ID，MemoryAdvisor 会自动从 ThreadLocal 读取。
     * 调用结束后必须调用 clear()。
     */
    public static void setConversationId(String conversationId) {
        CONVERSATION_ID_HOLDER.set(conversationId);
    }

    public static void clearConversationId() {
        CONVERSATION_ID_HOLDER.remove();
    }

    @Override
    public String getName() {
        return "memoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String conversationId = CONVERSATION_ID_HOLDER.get();
        if (conversationId == null || conversationId.isBlank()) {
            return chain.nextCall(request);
        }

        List<HistoryMessage> history = memoryService.getHistory(conversationId);
        String userText = request.prompt().getUserMessage().getText();

        ChatClientRequest enrichedRequest = request;
        if (!history.isEmpty()) {
            List<Message> messages = new ArrayList<>(request.prompt().getInstructions());
            int insertIdx = 1;
            int added = 0;
            for (HistoryMessage hm : history) {
                if ("user".equals(hm.role())) {
                    messages.add(insertIdx + added, new UserMessage(hm.content()));
                    added++;
                } else if ("assistant".equals(hm.role())) {
                    messages.add(insertIdx + added, new AssistantMessage(hm.content()));
                    added++;
                }
            }
            Prompt newPrompt = new Prompt(messages);
            enrichedRequest = request.mutate().prompt(newPrompt).build();
        }

        ChatClientResponse response = chain.nextCall(enrichedRequest);

        try {
            String assistantText = response.chatResponse().getResult().getOutput().getText();
            if (assistantText != null && !assistantText.isBlank()) {
                memoryService.saveExchange(conversationId, userText, assistantText);
            }
        } catch (Exception e) {
            log.warn("保存对话记忆失败: {}", e.getMessage());
        }

        return response;
    }
}

package com.xcodez.springaivoicecanvas.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptEnhanceAdvisor implements CallAdvisor {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的文生图提示词优化器。
            用户会用中文简单描述他们想要的图片。
            你需要将用户的需求扩展为高质量的英文提示词，直接返回优化后的英文提示词，不要任何其他文字。

            优化要求：
            1. 添加视觉细节：构图、光线、颜色、氛围
            2. 添加风格关键词：photorealistic / cinematic lighting / highly detailed / 4k / ultra realistic
            3. 保持用户原始意图不变，只做增强
            4. 只用英文输出，一行即可，不要换行
            """;

    @Override
    public String getName() {
        return "promptEnhanceAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt().getUserMessage().getText();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage(userText));

        Prompt newPrompt = new Prompt(messages);
        ChatClientRequest newRequest = request.mutate().prompt(newPrompt).build();
        return chain.nextCall(newRequest);
    }
}

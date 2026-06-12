package com.xcodez.springaivoicecanvas.advisor;

import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IntentAdvisor implements CallAdvisor {

    private static final String SYSTEM_PROMPT = """
            你是一个意图分类器。分析用户输入，判断用户的真实意图类型。

            分类规则（重要：默认倾向 image）：

            【diagram - 结构化图表】
            当用户明确要求：
            - 流程图、审批流程、工作流
            - ER图、实体关系图
            - 思维导图、脑图
            - 架构图、系统架构、微服务架构
            → 返回: diagram

            【shape - 自由绘图/矢量图形】
            仅当用户明确要求绘制具体形状时：
            - "画一个圆形"、"画一个红色矩形"、"画三角形"
            - "在画布上画线"、"画一个箭头"
            - 指定了位置、大小、颜色的具体几何图形指令
            → 返回: shape

            【image - AI创意生成（默认）】
            其余所有情况，包括但不限于：
            - 创意描述："画一只赛博朋克猫"、"夕阳下的海滩"
            - 具象画面："画一座雪山"、"画一个人物肖像"
            - 场景描述："创建一个未来城市"、"画星空"
            - 任何没有明确几何图形指令的绘画请求
            → 返回: image

            犹豫不决时返回 image。

            只返回一个单词（diagram / shape / image），不要任何其他文字。
            """;

    @Override
    public String getName() {
        return "intentAdvisor";
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

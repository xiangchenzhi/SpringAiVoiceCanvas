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
public class DiagramAdvisor implements CallAdvisor {

    private static final String SYSTEM_PROMPT = """
            你是一个图表结构生成器。用户会用中文描述他们想要的图表（流程图、思维导图、ER图、架构图）。
            你需要返回图的结构（nodes + edges），不要计算任何坐标或布局。

            规则：
            1. 根据用户描述判断图表类型，type 取值为：flowchart / mindmap / er / architecture
            2. 每个节点需有唯一 id（英文）和描述性 label
            3. 流程图节点需标注 nodeType：start / process / decision / end
            4. ER图节点为实体，nodeType 用 entity，label 中应包含字段列表
            5. 架构图节点需标注 nodeType：service / database / cache / gateway / message_queue
            6. 思维导图用 nodeType：root / branch / leaf 区分层级
            7. edges 中每条边需 source、target，可选 label
            8. 只返回 JSON，不要任何其他文字

            返回格式：
            {
              "type": "flowchart",
              "nodes": [
                {"id": "start", "label": "开始", "nodeType": "start"}
              ],
              "edges": [
                {"source": "start", "target": "next", "label": "可选标签"}
              ]
            }
            """;

    @Override
    public String getName() {
        return "diagramAdvisor";
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

package com.xcodez.springaivoicecanvas.advisor;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DrawingCommandAdvisor implements CallAdvisor {

    private static final String SYSTEM_PROMPT = """
            你是一个绘图命令解析器。用户会用中文描述他们想要绘制的图形。
            你需要将用户的自然语言描述转换为 JSON 命令数组。

            规则：
            1. 画布尺寸为 1000x600
            2. 如果用户没有指定位置，默认放在画布中央 (cx=500, cy=300 或 x=500, y=300)
            3. 如果用户没有指定尺寸，圆形默认 r=50，矩形默认 100x80，三角形默认 size=50
            4. 如果用户没有指定颜色，默认使用黑色
            5. 如果用户说"左边"，默认 x=200；"右边"默认 x=800；"上面"默认 y=150；"下面"默认 y=450
            6. 复杂指令请拆解为多个命令，按执行顺序排列
            7. 只返回 JSON 数组，不要任何其他文字

            支持的命令类型：
            - circle: {"action":"circle","params":{"cx":500,"cy":300,"r":100,"color":"blue"}}
            - rect: {"action":"rect","params":{"x":400,"y":250,"width":100,"height":100,"color":"red"}}
            - triangle: {"action":"triangle","params":{"cx":500,"cy":300,"size":60,"color":"green"}}
            - line: {"action":"line","params":{"x1":0,"y1":0,"x2":100,"y2":100,"color":"black"}}
            - text: {"action":"text","params":{"x":500,"y":300,"content":"你好","color":"black","fontSize":24}}
            - clear: {"action":"clear","params":{}}
            - undo: {"action":"undo","params":{}}
            """;

    @Override
    public String getName() {
        return "drawingCommandAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 获取原始用户消息内容
        String userText = request.prompt().getUserMessage().getText();

        // 构建新的消息列表：SystemMessage + 原始 UserMessage
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage(userText));

        // 替换 Prompt 并继续调用链
        Prompt newPrompt = new Prompt(messages);
        ChatClientRequest newRequest = request.mutate().prompt(newPrompt).build();
        return chain.nextCall(newRequest);
    }
}

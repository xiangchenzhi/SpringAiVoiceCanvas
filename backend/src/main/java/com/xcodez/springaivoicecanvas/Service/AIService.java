package com.xcodez.springaivoicecanvas.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AIService {
    private ChatClient chatClient;
    public String chat(String msg){
       return chatClient.prompt()
                .user(msg)
                .advisors()
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();
    }
}

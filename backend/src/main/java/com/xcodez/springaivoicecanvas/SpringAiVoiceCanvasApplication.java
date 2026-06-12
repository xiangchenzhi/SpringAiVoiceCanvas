package com.xcodez.springaivoicecanvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiVoiceCanvasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiVoiceCanvasApplication.class, args);
    }

}

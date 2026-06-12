package com.xcodez.springaivoicecanvas.controller;

import com.xcodez.springaivoicecanvas.Service.AIService;
import com.xcodez.springaivoicecanvas.model.ShapeCommand;
import com.xcodez.springaivoicecanvas.model.VoiceRequest;
import com.xcodez.springaivoicecanvas.model.VoiceResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VoiceController {

    private final AIService aiService;

    public VoiceController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/voice")
    public VoiceResponse handleVoice(@RequestBody VoiceRequest request) {
        List<ShapeCommand> commands = aiService.parseVoiceCommand(request.getTranscript(), "");
        return new VoiceResponse(commands, request.getTranscript());
    }
}

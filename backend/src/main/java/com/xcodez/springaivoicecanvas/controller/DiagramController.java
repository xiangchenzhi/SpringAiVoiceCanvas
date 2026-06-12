package com.xcodez.springaivoicecanvas.controller;

import com.xcodez.springaivoicecanvas.Service.AIService;
import com.xcodez.springaivoicecanvas.model.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DiagramController {

    private final AIService aiService;

    public DiagramController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/diagram")
    public DiagramResponse handleDiagram(@RequestBody DiagramRequest request) {
        Diagram diagram = aiService.parseDiagramCommand(request.getTranscript(), "");
        return new DiagramResponse("diagram", diagram, request.getTranscript());
    }
}

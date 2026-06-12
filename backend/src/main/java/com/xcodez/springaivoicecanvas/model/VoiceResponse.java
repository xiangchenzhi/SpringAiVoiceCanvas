package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceResponse {

    private List<ShapeCommand> commands;

    private String originalText;
}

package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRequest {

    private String transcript;

    private int canvasWidth = 1000;

    private int canvasHeight = 600;
}

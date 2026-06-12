package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramResponse {

    private String type = "diagram";

    private Diagram diagram;

    private String originalText;
}

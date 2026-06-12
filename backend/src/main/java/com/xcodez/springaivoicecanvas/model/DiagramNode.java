package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramNode {

    private String id;

    private String label;

    private String nodeType;
}

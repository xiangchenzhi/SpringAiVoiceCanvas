package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramEdge {

    private String source;

    private String target;

    private String label;
}

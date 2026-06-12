package com.xcodez.springaivoicecanvas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Diagram {

    private String type;

    private List<DiagramNode> nodes;

    private List<DiagramEdge> edges;
}

package com.xcodez.springaivoicecanvas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagram_versions")
public class DiagramVersion {

    @Id
    @Column(length = 36)
    private String id;

    /** 所属会话ID */
    @Column(length = 36)
    private String conversationId;

    /** 父版本ID，root 为 null */
    @Column(length = 36)
    private String parentVersionId;

    /** 用户当时输入的命令 */
    @Column(length = 500)
    private String command;

    /** AI 生成的摘要（一行） */
    @Column(length = 200)
    private String summary;

    /** 完整图表 JSON 快照 */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String diagramJson;

    /** 本次操作描述（增量，用于展示执行历史） */
    @Column(columnDefinition = "TEXT")
    private String operationJson;

    private LocalDateTime createdAt;

    public DiagramVersion() {
        this.createdAt = LocalDateTime.now();
    }

    public DiagramVersion(String id, String conversationId, String parentVersionId,
                          String command, String summary, String diagramJson, String operationJson) {
        this.id = id;
        this.conversationId = conversationId;
        this.parentVersionId = parentVersionId;
        this.command = command;
        this.summary = summary;
        this.diagramJson = diagramJson;
        this.operationJson = operationJson;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(String parentVersionId) { this.parentVersionId = parentVersionId; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDiagramJson() { return diagramJson; }
    public void setDiagramJson(String diagramJson) { this.diagramJson = diagramJson; }

    public String getOperationJson() { return operationJson; }
    public void setOperationJson(String operationJson) { this.operationJson = operationJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

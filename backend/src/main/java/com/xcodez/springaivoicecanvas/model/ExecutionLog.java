package com.xcodez.springaivoicecanvas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_logs")
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属版本ID */
    @Column(length = 36)
    private String versionId;

    /** 步骤名称（如"识别意图"、"提取实体"、"自动布局"） */
    @Column(length = 50)
    private String stepName;

    /** 步骤详细内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 排序，数字越小越靠前 */
    private Integer sortOrder;

    /** 日志级别: info / success / error / node */
    @Column(length = 10)
    private String level;

    private LocalDateTime createdAt;

    public ExecutionLog() {
        this.createdAt = LocalDateTime.now();
    }

    public ExecutionLog(String versionId, String stepName, String content, Integer sortOrder, String level) {
        this.versionId = versionId;
        this.stepName = stepName;
        this.content = content;
        this.sortOrder = sortOrder;
        this.level = level;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

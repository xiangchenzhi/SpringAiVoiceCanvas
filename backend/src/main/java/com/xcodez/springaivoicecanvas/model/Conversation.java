package com.xcodez.springaivoicecanvas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 200)
    private String title;

    /** DIAGRAM 或 IMAGE，决定版本树的语义 */
    @Column(length = 50)
    private String conversationType;

    /** 该会话最后一个图类型（流程图/ER图/架构图等），侧边栏预览用 */
    @Column(length = 50)
    private String lastType;

    /** 版本树的根版本ID */
    @Column(length = 36)
    private String rootVersionId;

    /** 当前工作版本ID */
    @Column(length = 36)
    private String currentVersionId;

    @Column(columnDefinition = "TEXT")
    private String lastImageUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Conversation() {}

    public Conversation(String id, String conversationType) {
        this.id = id;
        this.conversationType = conversationType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getConversationType() { return conversationType; }
    public void setConversationType(String conversationType) { this.conversationType = conversationType; }

    public String getLastType() { return lastType; }
    public void setLastType(String lastType) { this.lastType = lastType; }

    public String getRootVersionId() { return rootVersionId; }
    public void setRootVersionId(String rootVersionId) { this.rootVersionId = rootVersionId; }

    public String getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(String currentVersionId) { this.currentVersionId = currentVersionId; }

    public String getLastImageUrl() { return lastImageUrl; }
    public void setLastImageUrl(String lastImageUrl) { this.lastImageUrl = lastImageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

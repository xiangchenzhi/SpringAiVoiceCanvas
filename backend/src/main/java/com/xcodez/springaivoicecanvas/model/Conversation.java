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

    @Column(length = 20)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String lastImageUrl;

    @Column(columnDefinition = "TEXT")
    private String lastResult;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Conversation() {}

    public Conversation(String id, String type) {
        this.id = id;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // getters/setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLastImageUrl() { return lastImageUrl; }
    public void setLastImageUrl(String lastImageUrl) { this.lastImageUrl = lastImageUrl; }

    public String getLastResult() { return lastResult; }
    public void setLastResult(String lastResult) { this.lastResult = lastResult; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

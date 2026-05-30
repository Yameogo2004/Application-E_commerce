package model;

import java.time.LocalDateTime;

public class Notification {

    private int id;
    private String title;
    private String message;
    private String type;
    private String level;
    private boolean read;
    private String entityType;
    private Integer entityId;
    private LocalDateTime createdAt;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public Notification(int id, String title, String message, String type, String level,
                        boolean read, String entityType, Integer entityId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.level = level;
        this.read = read;
        this.entityType = entityType;
        this.entityId = entityId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    } 

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    } 

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    } 

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    } 

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    } 

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    } 

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    } 

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    } 

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
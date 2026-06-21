package com.votosync.vote.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(nullable = false, length = 64)
    private String key;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public IdempotencyKey() {
    }

    public IdempotencyKey(String key, Integer responseStatus, String responseBody, LocalDateTime createdAt) {
        this.key = key;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Manual Builder
    public static IdempotencyKeyBuilder builder() {
        return new IdempotencyKeyBuilder();
    }

    public static class IdempotencyKeyBuilder {
        private String key;
        private Integer responseStatus;
        private String responseBody;
        private LocalDateTime createdAt = LocalDateTime.now();

        public IdempotencyKeyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public IdempotencyKeyBuilder responseStatus(Integer responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }

        public IdempotencyKeyBuilder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public IdempotencyKeyBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IdempotencyKey build() {
            return new IdempotencyKey(key, responseStatus, responseBody, createdAt);
        }
    }
}

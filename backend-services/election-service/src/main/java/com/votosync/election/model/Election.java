package com.votosync.election.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "elections")
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, ACTIVE, COMPLETED

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Candidate> candidates = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Election() {
    }

    public Election(Long id, String title, String description, String status, LocalDateTime startDate, LocalDateTime endDate, List<Candidate> candidates, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        if (candidates != null) {
            this.candidates = candidates;
        }
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Manual Builder
    public static ElectionBuilder builder() {
        return new ElectionBuilder();
    }

    public static class ElectionBuilder {
        private Long id;
        private String title;
        private String description;
        private String status = "DRAFT";
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<Candidate> candidates = new ArrayList<>();
        private LocalDateTime createdAt = LocalDateTime.now();

        public ElectionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ElectionBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ElectionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ElectionBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ElectionBuilder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public ElectionBuilder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public ElectionBuilder candidates(List<Candidate> candidates) {
            this.candidates = candidates;
            return this;
        }

        public ElectionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Election build() {
            return new Election(id, title, description, status, startDate, endDate, candidates, createdAt);
        }
    }
}

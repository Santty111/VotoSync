package com.votosync.election.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id")
    @JsonIgnore
    private Election election;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String party;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Candidate() {
    }

    public Candidate(Long id, Election election, String name, String party, String photoUrl, LocalDateTime createdAt) {
        this.id = id;
        this.election = election;
        this.name = name;
        this.party = party;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", party='" + party + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // Manual Builder
    public static CandidateBuilder builder() {
        return new CandidateBuilder();
    }

    public static class CandidateBuilder {
        private Long id;
        private Election election;
        private String name;
        private String party;
        private String photoUrl;
        private LocalDateTime createdAt = LocalDateTime.now();

        public CandidateBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CandidateBuilder election(Election election) {
            this.election = election;
            return this;
        }

        public CandidateBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CandidateBuilder party(String party) {
            this.party = party;
            return this;
        }

        public CandidateBuilder photoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public CandidateBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Candidate build() {
            return new Candidate(id, election, name, party, photoUrl, createdAt);
        }
    }
}

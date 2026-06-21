package com.votosync.vote.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "citizen_id_hash", nullable = false, length = 64)
    private String citizenIdHash;

    @Column(name = "transaction_hash", unique = true, nullable = false, length = 64)
    private String transactionHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String signature;

    @Column(name = "logical_timestamp", nullable = false)
    private Long logicalTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Vote() {
    }

    public Vote(Long id, Long electionId, Long candidateId, String citizenIdHash, String transactionHash, String signature, Long logicalTimestamp, LocalDateTime createdAt) {
        this.id = id;
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.citizenIdHash = citizenIdHash;
        this.transactionHash = transactionHash;
        this.signature = signature;
        this.logicalTimestamp = logicalTimestamp;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCitizenIdHash() {
        return citizenIdHash;
    }

    public void setCitizenIdHash(String citizenIdHash) {
        this.citizenIdHash = citizenIdHash;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getLogicalTimestamp() {
        return logicalTimestamp;
    }

    public void setLogicalTimestamp(Long logicalTimestamp) {
        this.logicalTimestamp = logicalTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Manual Builder
    public static VoteBuilder builder() {
        return new VoteBuilder();
    }

    public static class VoteBuilder {
        private Long id;
        private Long electionId;
        private Long candidateId;
        private String citizenIdHash;
        private String transactionHash;
        private String signature;
        private Long logicalTimestamp;
        private LocalDateTime createdAt = LocalDateTime.now();

        public VoteBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VoteBuilder electionId(Long electionId) {
            this.electionId = electionId;
            return this;
        }

        public VoteBuilder candidateId(Long candidateId) {
            this.candidateId = candidateId;
            return this;
        }

        public VoteBuilder citizenIdHash(String citizenIdHash) {
            this.citizenIdHash = citizenIdHash;
            return this;
        }

        public VoteBuilder transactionHash(String transactionHash) {
            this.transactionHash = transactionHash;
            return this;
        }

        public VoteBuilder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public VoteBuilder logicalTimestamp(Long logicalTimestamp) {
            this.logicalTimestamp = logicalTimestamp;
            return this;
        }

        public VoteBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Vote build() {
            return new Vote(id, electionId, candidateId, citizenIdHash, transactionHash, signature, logicalTimestamp, createdAt);
        }
    }
}

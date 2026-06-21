package com.votosync.audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
public class AuditVote {

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
    private LocalDateTime createdAt;

    // Constructors
    public AuditVote() {
    }

    public AuditVote(Long id, Long electionId, Long candidateId, String citizenIdHash, String transactionHash, String signature, Long logicalTimestamp, LocalDateTime createdAt) {
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
    public static AuditVoteBuilder builder() {
        return new AuditVoteBuilder();
    }

    public static class AuditVoteBuilder {
        private Long id;
        private Long electionId;
        private Long candidateId;
        private String citizenIdHash;
        private String transactionHash;
        private String signature;
        private Long logicalTimestamp;
        private LocalDateTime createdAt;

        public AuditVoteBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditVoteBuilder electionId(Long electionId) {
            this.electionId = electionId;
            return this;
        }

        public AuditVoteBuilder candidateId(Long candidateId) {
            this.candidateId = candidateId;
            return this;
        }

        public AuditVoteBuilder citizenIdHash(String citizenIdHash) {
            this.citizenIdHash = citizenIdHash;
            return this;
        }

        public AuditVoteBuilder transactionHash(String transactionHash) {
            this.transactionHash = transactionHash;
            return this;
        }

        public AuditVoteBuilder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public AuditVoteBuilder logicalTimestamp(Long logicalTimestamp) {
            this.logicalTimestamp = logicalTimestamp;
            return this;
        }

        public AuditVoteBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditVote build() {
            return new AuditVote(id, electionId, candidateId, citizenIdHash, transactionHash, signature, logicalTimestamp, createdAt);
        }
    }
}

package com.votosync.vote.dto;

import java.io.Serializable;

public class VotePayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long electionId;
    private Long candidateId;
    private String nationalId;
    private String signature;
    private String authToken;
    private Long logicalTimestamp;

    // Constructors
    public VotePayload() {
    }

    public VotePayload(Long electionId, Long candidateId, String nationalId, String signature, String authToken, Long logicalTimestamp) {
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.nationalId = nationalId;
        this.signature = signature;
        this.authToken = authToken;
        this.logicalTimestamp = logicalTimestamp;
    }

    // Getters and Setters
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

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Long getLogicalTimestamp() {
        return logicalTimestamp;
    }

    public void setLogicalTimestamp(Long logicalTimestamp) {
        this.logicalTimestamp = logicalTimestamp;
    }
}

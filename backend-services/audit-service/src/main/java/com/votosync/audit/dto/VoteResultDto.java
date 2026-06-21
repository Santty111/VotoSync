package com.votosync.audit.dto;

public class VoteResultDto {
    private Long candidateId;
    private Long voteCount;

    // Constructors
    public VoteResultDto() {
    }

    public VoteResultDto(Long candidateId, Long voteCount) {
        this.candidateId = candidateId;
        this.voteCount = voteCount;
    }

    // Getters and Setters
    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }
}

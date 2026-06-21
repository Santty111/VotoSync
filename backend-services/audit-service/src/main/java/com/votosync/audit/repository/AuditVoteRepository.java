package com.votosync.audit.repository;

import com.votosync.audit.dto.VoteResultDto;
import com.votosync.audit.model.AuditVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditVoteRepository extends JpaRepository<AuditVote, Long> {
    
    List<AuditVote> findByElectionId(Long electionId);
    
    List<AuditVote> findFirst50ByOrderByLogicalTimestampDesc();

    @Query("SELECT new com.votosync.audit.dto.VoteResultDto(v.candidateId, COUNT(v)) " +
           "FROM AuditVote v " +
           "WHERE v.electionId = :electionId " +
           "GROUP BY v.candidateId")
    List<VoteResultDto> countVotesByCandidate(@Param("electionId") Long electionId);
}

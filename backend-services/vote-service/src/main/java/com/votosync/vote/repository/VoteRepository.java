package com.votosync.vote.repository;

import com.votosync.vote.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByElectionIdAndCitizenIdHash(Long electionId, String citizenIdHash);
    Optional<Vote> findFirstByOrderByLogicalTimestampDesc();
}

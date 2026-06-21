package com.votosync.audit.controller;

import com.votosync.audit.dto.VoteResultDto;
import com.votosync.audit.model.AuditVote;
import com.votosync.audit.repository.AuditVoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditVoteRepository auditVoteRepository;

    // Manual Injection Constructor
    public AuditController(AuditVoteRepository auditVoteRepository) {
        this.auditVoteRepository = auditVoteRepository;
    }

    @GetMapping("/ledger")
    public List<AuditVote> getLedger() {
        log.info("Fetching global public audit ledger (last 50 logs)");
        return auditVoteRepository.findFirst50ByOrderByLogicalTimestampDesc();
    }

    @GetMapping("/ledger/election/{electionId}")
    public List<AuditVote> getElectionLedger(@PathVariable Long electionId) {
        log.info("Fetching public audit ledger for election ID: {}", electionId);
        return auditVoteRepository.findByElectionId(electionId);
    }

    @GetMapping("/results/{electionId}")
    public List<VoteResultDto> getResults(@PathVariable Long electionId) {
        log.info("Fetching real-time results for election ID: {}", electionId);
        return auditVoteRepository.countVotesByCandidate(electionId);
    }
}

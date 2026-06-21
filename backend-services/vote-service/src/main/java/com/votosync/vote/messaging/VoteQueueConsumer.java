package com.votosync.vote.messaging;

import com.votosync.vote.config.RabbitMQConfig;
import com.votosync.vote.dto.VotePayload;
import com.votosync.vote.model.Vote;
import com.votosync.vote.repository.VoteRepository;
import com.votosync.vote.service.VoteRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class VoteQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(VoteQueueConsumer.class);

    private final VoteRepository voteRepository;
    private final VoteRegistrationService voteRegistrationService;
    private final RestTemplate restTemplate;

    @Value("${services.identity.url}")
    private String identityServiceUrl;

    // Manual Injection Constructor
    public VoteQueueConsumer(VoteRepository voteRepository, VoteRegistrationService voteRegistrationService, RestTemplate restTemplate) {
        this.voteRepository = voteRepository;
        this.voteRegistrationService = voteRegistrationService;
        this.restTemplate = restTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.VOTE_QUEUE)
    public void receiveVote(VotePayload payload) {
        log.info("Consumer: Processing vote from queue for election {} and candidate {}", 
                 payload.getElectionId(), payload.getCandidateId());
        
        try {
            String citizenHash = voteRegistrationService.hashString(payload.getNationalId());

            // 1. Double safety check: Verify citizen hasn't already voted
            if (voteRepository.existsByElectionIdAndCitizenIdHash(payload.getElectionId(), citizenHash)) {
                log.warn("Consumer: Duplicate vote detected for citizen hash: {}. Discarding.", citizenHash);
                return;
            }

            // 2. Compute logical timestamp to maintain absolute sequence order
            long logicalTime = computeLogicalTimestamp(payload.getLogicalTimestamp());

            // 3. Generate transaction hash for the public ledger validation
            String rawDetails = citizenHash + ":" 
                    + payload.getElectionId() + ":" 
                    + payload.getCandidateId() + ":" 
                    + payload.getSignature() + ":" 
                    + logicalTime;
            String transactionHash = voteRegistrationService.hashString(rawDetails);

            // 4. Build and save vote entity
            Vote vote = Vote.builder()
                    .electionId(payload.getElectionId())
                    .candidateId(payload.getCandidateId())
                    .citizenIdHash(citizenHash)
                    .transactionHash(transactionHash)
                    .signature(payload.getSignature())
                    .logicalTimestamp(logicalTime)
                    .build();

            voteRepository.save(vote);
            log.info("Consumer: Vote stored successfully in Master database. TxHash: {}", transactionHash);

            // 5. Notify Identity Service to lock citizen profile (mark hasVoted = true)
            notifyIdentityService(payload.getNationalId());

        } catch (Exception e) {
            log.error("Consumer: Critical error processing queued vote", e);
            throw new RuntimeException("Re-queueing vote due to error", e);
        }
    }

    private synchronized long computeLogicalTimestamp(Long messageTimestamp) {
        Optional<Vote> lastVote = voteRepository.findFirstByOrderByLogicalTimestampDesc();
        long lastTimestamp = lastVote.map(Vote::getLogicalTimestamp).orElse(0L);
        long incoming = messageTimestamp != null ? messageTimestamp : System.currentTimeMillis();
        return Math.max(incoming, lastTimestamp + 1);
    }

    private void notifyIdentityService(String nationalId) {
        try {
            String url = identityServiceUrl + "/api/auth/mark-voted?nationalId=" + nationalId;
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Consumer: Identity Service notified. Citizen status marked as voted.");
            } else {
                log.warn("Consumer: Failed to update citizen status in Identity Service: Status {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Consumer: Error calling Identity Service: {}", e.getMessage());
        }
    }
}

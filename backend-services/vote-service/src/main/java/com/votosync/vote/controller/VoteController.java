package com.votosync.vote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votosync.vote.dto.VotePayload;
import com.votosync.vote.model.IdempotencyKey;
import com.votosync.vote.repository.IdempotencyKeyRepository;
import com.votosync.vote.service.VoteRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/votes")
@CrossOrigin(origins = "*")
public class VoteController {

    private static final Logger log = LoggerFactory.getLogger(VoteController.class);

    private final VoteRegistrationService voteRegistrationService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Manual Injection Constructor
    public VoteController(VoteRegistrationService voteRegistrationService, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.voteRegistrationService = voteRegistrationService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @PostMapping
    public ResponseEntity<?> submitVote(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody VotePayload payload) {

        log.info("Received vote request for election: {}, candidate: {}", 
                 payload.getElectionId(), payload.getCandidateId());

        if (idempotencyKeyHeader == null || idempotencyKeyHeader.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required HTTP Header: Idempotency-Key");
        }

        // 1. Check Idempotency Key cache
        Optional<IdempotencyKey> cachedResponse = idempotencyKeyRepository.findById(idempotencyKeyHeader);
        if (cachedResponse.isPresent()) {
            log.warn("Duplicate request detected with Idempotency Key: {}. Returning cached result.", idempotencyKeyHeader);
            IdempotencyKey keyRecord = cachedResponse.get();
            try {
                Map<?, ?> body = objectMapper.readValue(keyRecord.getResponseBody(), Map.class);
                return ResponseEntity.status(keyRecord.getResponseStatus()).body(body);
            } catch (Exception e) {
                return ResponseEntity.status(keyRecord.getResponseStatus()).body(keyRecord.getResponseBody());
            }
        }

        // 2. Perform validations
        // A. Validate citizen authorization token
        boolean isTokenValid = voteRegistrationService.validateUserToken(payload.getAuthToken(), payload.getNationalId());
        if (!isTokenValid) {
            return saveAndReturn(idempotencyKeyHeader, HttpStatus.UNAUTHORIZED, "Invalid or expired authorization token");
        }

        // B. Validate election status
        boolean isElectionActive = voteRegistrationService.isElectionActive(payload.getElectionId());
        if (!isElectionActive) {
            return saveAndReturn(idempotencyKeyHeader, HttpStatus.BAD_REQUEST, "Election is not active or does not exist");
        }

        // C. Check if voter already registered a vote in the database
        boolean alreadyVoted = voteRegistrationService.citizenAlreadyVoted(payload.getElectionId(), payload.getNationalId());
        if (alreadyVoted) {
            return saveAndReturn(idempotencyKeyHeader, HttpStatus.CONFLICT, "Citizen has already registered a vote for this election");
        }

        // 3. Queue the vote payload to RabbitMQ
        try {
            voteRegistrationService.queueVote(payload);
        } catch (Exception e) {
            log.error("Failed to queue vote payload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Broker unavailable, failed to buffer vote");
        }

        // 4. Return success status (queued)
        return saveAndReturn(idempotencyKeyHeader, HttpStatus.ACCEPTED, "Vote queued successfully for ledger processing");
    }

    private ResponseEntity<?> saveAndReturn(String key, HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            IdempotencyKey cache = IdempotencyKey.builder()
                    .key(key)
                    .responseStatus(status.value())
                    .responseBody(jsonBody)
                    .build();
            idempotencyKeyRepository.save(cache);
        } catch (Exception e) {
            log.error("Failed to write to idempotency key repository", e);
        }

        return ResponseEntity.status(status).body(body);
    }
}

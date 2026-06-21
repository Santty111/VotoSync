package com.votosync.vote.service;

import com.votosync.vote.config.RabbitMQConfig;
import com.votosync.vote.dto.VotePayload;
import com.votosync.vote.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class VoteRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(VoteRegistrationService.class);

    private final RabbitTemplate rabbitTemplate;
    private final VoteRepository voteRepository;
    private final RestTemplate restTemplate;

    @Value("${services.identity.url}")
    private String identityServiceUrl;

    @Value("${services.election.url}")
    private String electionServiceUrl;

    // Manual Injection Constructor
    public VoteRegistrationService(RabbitTemplate rabbitTemplate, VoteRepository voteRepository, RestTemplate restTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.voteRepository = voteRepository;
        this.restTemplate = restTemplate;
    }

    public void queueVote(VotePayload payload) {
        log.info("Queueing vote payload to RabbitMQ for election: {}", payload.getElectionId());
        
        // Populate logical timestamp if not present
        if (payload.getLogicalTimestamp() == null) {
            payload.setLogicalTimestamp(System.currentTimeMillis());
        }
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VOTE_EXCHANGE,
                RabbitMQConfig.VOTE_ROUTING_KEY,
                payload
        );
        log.info("Vote payload published successfully to exchange: {}", RabbitMQConfig.VOTE_EXCHANGE);
    }

    public boolean validateUserToken(String token, String username) {
        try {
            String url = identityServiceUrl + "/api/auth/validate-token?token=" + token + "&username=" + username;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("valid"));
            }
        } catch (Exception e) {
            log.error("Failed to communicate with Identity Service: {}", e.getMessage());
        }
        return false;
    }

    public boolean isElectionActive(Long electionId) {
        try {
            String url = electionServiceUrl + "/api/elections/" + electionId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                return "ACTIVE".equalsIgnoreCase(status);
            }
        } catch (Exception e) {
            log.error("Failed to communicate with Election Service: {}", e.getMessage());
        }
        return false;
    }

    public boolean citizenAlreadyVoted(Long electionId, String nationalId) {
        String citizenHash = hashString(nationalId);
        return voteRepository.existsByElectionIdAndCitizenIdHash(electionId, citizenHash);
    }

    public String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }
}

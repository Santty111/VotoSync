package com.votosync.election.controller;

import com.votosync.election.model.Candidate;
import com.votosync.election.model.Election;
import com.votosync.election.repository.CandidateRepository;
import com.votosync.election.repository.ElectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/elections")
@CrossOrigin(origins = "*")
public class ElectionController {

    private static final Logger log = LoggerFactory.getLogger(ElectionController.class);

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;

    // Manual Injection Constructor
    public ElectionController(ElectionRepository electionRepository, CandidateRepository candidateRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @GetMapping
    public List<Election> getAllElections() {
        log.info("Fetching all elections");
        return electionRepository.findAll();
    }

    @GetMapping("/active")
    public List<Election> getActiveElections() {
        log.info("Fetching active elections");
        return electionRepository.findByStatus("ACTIVE");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Election> getElectionById(@PathVariable("id") Long id) {
        log.info("Fetching election details for ID: {}", id);
        return electionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Election> createElection(@RequestBody Election election) {
        log.info("Creating election: {}", election.getTitle());
        Election savedElection = electionRepository.save(election);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedElection);
    }

    @PostMapping("/{id}/candidates")
    public ResponseEntity<Candidate> addCandidate(@PathVariable("id") Long id, @RequestBody Candidate candidate) {
        log.info("Adding candidate '{}' to election ID: {}", candidate.getName(), id);
        Optional<Election> electionOpt = electionRepository.findById(id);
        if (electionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        candidate.setElection(electionOpt.get());
        Candidate savedCandidate = candidateRepository.save(candidate);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCandidate);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Election> updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        log.info("Updating status of election ID: {} to {}", id, status);
        Optional<Election> electionOpt = electionRepository.findById(id);
        if (electionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Election election = electionOpt.get();
        election.setStatus(status.toUpperCase());
        Election updatedElection = electionRepository.save(election);
        return ResponseEntity.ok(updatedElection);
    }
}

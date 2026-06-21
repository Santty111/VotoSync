package com.votosync.identity.repository;

import com.votosync.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNationalId(String nationalId);
    Optional<User> findByEmail(String email);
}

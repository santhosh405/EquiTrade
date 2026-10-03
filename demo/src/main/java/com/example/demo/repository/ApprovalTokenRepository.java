package com.example.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.ApprovalToken;

@Repository
public interface ApprovalTokenRepository extends JpaRepository<ApprovalToken, Long> {

    Optional<ApprovalToken> findByToken(String token);
}

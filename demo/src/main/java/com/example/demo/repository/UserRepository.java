package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;
import com.example.demo.model.enums.AccessStatus;
import com.example.demo.model.enums.AccountStatus;
import com.example.demo.model.enums.KycStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByAccessStatus(AccessStatus accessStatus);

    long countByAccessStatus(AccessStatus accessStatus);

    long countByKycStatus(KycStatus kycStatus);

    long countByAccountStatus(AccountStatus accountStatus);

    List<User> findByKycStatus(KycStatus kycStatus);

    List<User> findByRole(String role);
}
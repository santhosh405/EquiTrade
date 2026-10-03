package com.example.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.UserLoginHistory;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {

    List<UserLoginHistory> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserLoginHistory> findByEmailOrderByTimestampDesc(String email);

    List<UserLoginHistory> findBySuspiciousTrueOrderByTimestampDesc();

    Page<UserLoginHistory> findAllByOrderByTimestampDesc(Pageable pageable);
}

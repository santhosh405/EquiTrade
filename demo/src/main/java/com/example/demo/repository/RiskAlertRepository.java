package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.RiskAlert;
import com.example.demo.model.enums.RiskAlertStatus;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findAllByOrderByCreatedAtDesc();

    List<RiskAlert> findByStatusOrderByCreatedAtDesc(RiskAlertStatus status);

    long countByStatus(RiskAlertStatus status);
}

package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.SecurityIncident;
import com.example.demo.model.enums.SecurityIncidentStatus;

@Repository
public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, Long> {

    List<SecurityIncident> findAllByOrderByCreatedAtDesc();

    List<SecurityIncident> findByStatusOrderByCreatedAtDesc(SecurityIncidentStatus status);

    long countByStatus(SecurityIncidentStatus status);
}

package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.RegulatoryCommunication;

@Repository
public interface RegulatoryCommunicationRepository extends JpaRepository<RegulatoryCommunication, Long> {

    List<RegulatoryCommunication> findAllByOrderByTimestampDesc();
}

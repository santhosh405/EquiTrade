package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.FeeRule;

@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {

    List<FeeRule> findAllByOrderByVersionDesc();

    Optional<FeeRule> findFirstByActiveTrueOrderByVersionDesc();
}

package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.KycTimeline;
import com.example.demo.model.User;

@Repository
public interface KycTimelineRepository extends JpaRepository<KycTimeline, Long> {

    List<KycTimeline> findByUserOrderByTimestampDesc(User user);
}

package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.KycDocument;
import com.example.demo.model.User;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByUserOrderByUploadedAtDesc(User user);

    List<KycDocument> findByStatus(String status);
}

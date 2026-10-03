package com.example.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<WalletTransaction> findAllByOrderByCreatedAtDesc();

    Page<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<WalletTransaction> findByTypeOrderByCreatedAtDesc(String type);

    List<WalletTransaction> findByStatusOrderByCreatedAtDesc(String status);
}

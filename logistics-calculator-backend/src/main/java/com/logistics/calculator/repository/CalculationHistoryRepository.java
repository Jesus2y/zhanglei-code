package com.logistics.calculator.repository;

import com.logistics.calculator.model.CalculationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationHistoryRepository extends JpaRepository<CalculationHistory, Long> {
    Page<CalculationHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

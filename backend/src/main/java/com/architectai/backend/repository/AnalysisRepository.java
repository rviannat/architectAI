package com.architectai.backend.repository;

import com.architectai.backend.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, String> {
    List<Analysis> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<Analysis> findByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
}


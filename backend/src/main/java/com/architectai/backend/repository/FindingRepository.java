package com.architectai.backend.repository;

import com.architectai.backend.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, String> {
    List<Finding> findByAnalysisId(String analysisId);
}


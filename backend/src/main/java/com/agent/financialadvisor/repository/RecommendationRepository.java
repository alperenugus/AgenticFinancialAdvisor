package com.agent.financialadvisor.repository;

import com.agent.financialadvisor.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Recommendation> findByUserIdAndSymbol(String userId, String symbol);
    List<Recommendation> findByUserIdAndActionOrderByCreatedAtDesc(String userId, Recommendation.RecommendationAction action);
}



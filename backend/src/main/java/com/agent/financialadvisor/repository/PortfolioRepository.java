package com.agent.financialadvisor.repository;

import com.agent.financialadvisor.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(String userId);
    boolean existsByUserId(String userId);
}



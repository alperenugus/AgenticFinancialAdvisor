package com.agent.financialadvisor.repository;

import com.agent.financialadvisor.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(String userId);

    /**
     * Fetch portfolio with holdings eagerly loaded via JOIN FETCH.
     * Use this when accessing holdings outside the original transaction/session
     * (e.g. when tools are invoked from agent thread pool) to avoid LazyInitializationException.
     */
    @Query("SELECT DISTINCT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.userId = :userId")
    Optional<Portfolio> findByUserIdWithHoldings(@Param("userId") String userId);

    boolean existsByUserId(String userId);
}



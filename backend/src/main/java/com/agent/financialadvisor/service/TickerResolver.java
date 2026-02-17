package com.agent.financialadvisor.service;

import java.util.List;

public interface TickerResolver {

    Decision resolve(String userInput, List<Candidate> candidates);

    record Candidate(String symbol, String description, String type, boolean directQuoteCandidate) { }

    record Decision(String symbol, boolean accepted, String reason) { }
}

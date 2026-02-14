package com.agent.financialadvisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinancialAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialAdvisorApplication.class, args);
	}

}



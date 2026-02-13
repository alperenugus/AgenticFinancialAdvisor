package com.agent.financialadvisor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration for DataSource that handles Railway's DATABASE_URL format.
 * Railway provides DATABASE_URL as: postgresql://user:pass@host:port/dbname
 * Spring Boot expects: jdbc:postgresql://host:port/dbname
 */
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        // If DATABASE_URL is provided (Railway), parse and convert it
        if (StringUtils.hasText(databaseUrl) && !databaseUrl.startsWith("jdbc:")) {
            try {
                // Parse Railway's postgresql:// format
                // Example: postgresql://user:pass@host:port/dbname
                URI dbUri = new URI(databaseUrl);
                
                // Extract components
                String username = dbUri.getUserInfo() != null ? dbUri.getUserInfo().split(":")[0] : "postgres";
                String password = dbUri.getUserInfo() != null && dbUri.getUserInfo().split(":").length > 1 
                    ? dbUri.getUserInfo().split(":")[1] : "";
                String host = dbUri.getHost();
                int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;
                String database = dbUri.getPath() != null ? dbUri.getPath().replaceFirst("/", "") : "railway";
                
                // Convert to JDBC format
                String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
                
                // Build DataSource with parsed values
                return DataSourceBuilder.create()
                        .url(jdbcUrl)
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();
                
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid DATABASE_URL format: " + databaseUrl, e);
            }
        } else if (StringUtils.hasText(databaseUrl) && databaseUrl.startsWith("jdbc:")) {
            // Already in JDBC format, use as-is
            return DataSourceBuilder.create()
                    .url(databaseUrl)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }
        
        // Otherwise, use default from application.yml via DataSourceProperties
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}


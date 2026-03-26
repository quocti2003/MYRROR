package com.mirror.product.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RdsSecretsManagerUtil {

    private static final String DEFAULT_SECRET_NAME = "rds!db-3c41e561-738d-4ff2-8e5f-ed6649af51b1";
    private static final String DEFAULT_REGION = "ap-southeast-1";
    
    private String secretName;
    private Region region;
    private DatabaseCredentials cachedCredentials;
    
    public RdsSecretsManagerUtil() {
        this.secretName = System.getenv("RDS_SECRET_NAME") != null ? 
            System.getenv("RDS_SECRET_NAME") : DEFAULT_SECRET_NAME;
        String regionStr = System.getenv("AWS_REGION") != null ? 
            System.getenv("AWS_REGION") : DEFAULT_REGION;
        this.region = Region.of(regionStr);
    }
    
    public RdsSecretsManagerUtil(String secretName, String regionStr) {
        this.secretName = secretName;
        this.region = Region.of(regionStr);
    }
    
    public DatabaseCredentials getCredentialsFromSecretsManager() {
        if (cachedCredentials != null) {
            return cachedCredentials;
        }
        
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build()) {
            
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
            String secret = getSecretValueResponse.secretString();
            
            JSONObject json = new JSONObject(secret);
            
            String host = System.getProperty("rds.host", 
                System.getenv("RDS_HOST") != null ? System.getenv("RDS_HOST") : 
                "mirror-user-db.ch0a2gs8awvf.ap-southeast-1.rds.amazonaws.com");
            
            int port = Integer.parseInt(System.getProperty("rds.port", 
                System.getenv("RDS_PORT") != null ? System.getenv("RDS_PORT") : "5432"));
            
            String dbname = System.getProperty("rds.dbname", 
                System.getenv("RDS_DBNAME") != null ? System.getenv("RDS_DBNAME") : "mirror_product");
            
            cachedCredentials = DatabaseCredentials.builder()
                    .username(json.getString("username"))
                    .password(json.getString("password"))
                    .host(host)
                    .port(port)
                    .dbname(dbname)
                    .build();
            
            log.info("Successfully retrieved database credentials from Secrets Manager");
            log.info("Using RDS endpoint: {}:{}/{}", host, port, dbname);
            return cachedCredentials;
            
        } catch (Exception e) {
            log.error("Failed to retrieve credentials from Secrets Manager: {}", e.getMessage());
            throw new RuntimeException("Unable to connect to Secrets Manager", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        DatabaseCredentials credentials = getCredentialsFromSecretsManager();
        String url = String.format("jdbc:postgresql://%s:%d/%s", 
            credentials.getHost(), 
            credentials.getPort(), 
            credentials.getDbname());
        
        return DriverManager.getConnection(url, credentials.getUsername(), credentials.getPassword());
    }
    
    public DataSource createDataSource() {
        DatabaseCredentials credentials = getCredentialsFromSecretsManager();
        String url = String.format("jdbc:postgresql://%s:%d/%s", 
            credentials.getHost(), 
            credentials.getPort(), 
            credentials.getDbname());
        
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(credentials.getUsername())
                .password(credentials.getPassword())
                .build();
    }
    
    public void testConnection() {
        try (Connection conn = getConnection()) {
            log.info("✅ Connected to RDS successfully!");
            log.info("Database: {}", conn.getMetaData().getDatabaseProductName());
            log.info("Version: {}", conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            log.error("❌ Failed to connect to RDS: {}", e.getMessage());
            throw new RuntimeException("Database connection test failed", e);
        }
    }
    
    public void clearCache() {
        cachedCredentials = null;
        log.info("Credentials cache cleared");
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DatabaseCredentials {
        private String username;
        private String password;
        private String host;
        private int port;
        private String dbname;
    }
}
package com.mirror.product.config;

import com.mirror.product.util.RdsSecretsManagerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "datasource.use-secrets-manager", havingValue = "true", matchIfMissing = false)
public class RdsDataSourceConfig {
    
    @Autowired
    private RdsSecretsManagerUtil rdsSecretsManagerUtil;
    
    @Value("${rds.secret.name:rds!db-3c41e561-738d-4ff2-8e5f-ed6649af51b1}")
    private String secretName;
    
    @Value("${rds.secret.region:ap-southeast-1}")
    private String region;
    
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Configuring DataSource using AWS Secrets Manager");
        log.info("Secret Name: {}, Region: {}", secretName, region);
        
        try {
            DataSource dataSource = rdsSecretsManagerUtil.createDataSource();
            
            rdsSecretsManagerUtil.testConnection();
            
            log.info("DataSource successfully configured with AWS Secrets Manager");
            return dataSource;
            
        } catch (Exception e) {
            log.error("Failed to configure DataSource with Secrets Manager: {}", e.getMessage());
            log.error("Please check your AWS credentials and secret configuration");
            throw new RuntimeException("Unable to configure DataSource", e);
        }
    }
    
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.mirror.product.entity", "com.mirror.product.model");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true");
        
        em.setJpaProperties(properties);
        
        return em;
    }
    
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
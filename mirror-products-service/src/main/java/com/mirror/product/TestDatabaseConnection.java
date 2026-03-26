package com.mirror.product;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseConnection {
    
    public static void main(String[] args) {
        String[] configs = {
            // PostgreSQL với các password khả năng
            "jdbc:postgresql://localhost:5432/todoList|postgres|123456",
            "jdbc:postgresql://localhost:5432/todoList|postgres|postgres", 
            "jdbc:postgresql://localhost:5432/todoList|postgres|",
            "jdbc:postgresql://localhost:5432/todoList|postgres|admin",
            // H2 fallback
            "jdbc:h2:mem:testdb|sa|password"
        };
        
        for (String config : configs) {
            String[] parts = config.split("\\|");
            String url = parts[0];
            String username = parts[1];
            String password = parts.length > 2 ? parts[2] : "";
            
            System.out.println("\n=== Testing connection ===");
            System.out.println("URL: " + url);
            System.out.println("Username: " + username);
            System.out.println("Password: " + (password.isEmpty() ? "[empty]" : "[" + password.length() + " chars]"));
            
            try {
                Connection connection = DriverManager.getConnection(url, username, password);
                System.out.println("✅ Connection successful!");
                
                // Test query
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1 as test");
                if (resultSet.next()) {
                    System.out.println("✅ Query test successful: " + resultSet.getInt("test"));
                }
                
                connection.close();
                System.out.println("✅ This configuration works! Update your application.properties:");
                System.out.println("spring.datasource.url=" + url);
                System.out.println("spring.datasource.username=" + username);
                System.out.println("spring.datasource.password=" + password);
                break;
                
            } catch (Exception e) {
                System.out.println("❌ Connection failed: " + e.getMessage());
            }
        }
    }
}
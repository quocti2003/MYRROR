package com.mirror.product.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * One-time utility to fix Liquibase checksum validation error
 * Run this once to clear all checksums, then delete this file
 */
public class FixLiquibaseChecksum {

    public static void main(String[] args) {
        String url = "jdbc:postgresql://mirror-user-db.ch0a2gs8awvf.ap-southeast-1.rds.amazonaws.com:5432/mirror_product";
        String user = "mirror_user";
        String password = "2v~]Tob:<epZzN3e:lF5zAGWbnz3";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to database successfully!");

            // Clear all checksums
            int rowsUpdated = stmt.executeUpdate("UPDATE databasechangelog SET md5sum = NULL");

            System.out.println("✓ Successfully cleared checksums for " + rowsUpdated + " changesets");
            System.out.println("✓ You can now restart the application");
            System.out.println("✓ Liquibase will recalculate checksums automatically");

        } catch (Exception e) {
            System.err.println("✗ Error connecting to database:");
            e.printStackTrace();
        }
    }
}

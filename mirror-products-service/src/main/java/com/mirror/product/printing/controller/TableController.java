package com.mirror.product.printing.controller;

import com.mirror.product.printing.dto.PrintingApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/printing")
public class TableController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/tables")
    public ResponseEntity<PrintingApiResponse> listTables(@RequestParam("db") String database) {
        if (database == null || database.isBlank()) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Missing parameter", "Parameter 'db' is required"));
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_name";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }

                PrintingApiResponse response = PrintingApiResponse.success();
                response.setDatabase(database);
                response.setTables(tables);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(PrintingApiResponse.error("Database error", e.getMessage()));
        }
    }
}

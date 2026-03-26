package com.mirror.product.printing.controller;

import com.mirror.product.printing.dto.PrintingApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/printing")
public class DataController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/data")
    public ResponseEntity<PrintingApiResponse> getData(
            @RequestParam("db") String database,
            @RequestParam("table") String table,
            @RequestParam(value = "columns", required = false) String columns,
            @RequestParam(value = "limit", required = false) Integer limit) {

        if (database == null || database.isBlank()) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Missing parameter", "Parameter 'db' is required"));
        }
        if (table == null || table.isBlank()) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Missing parameter", "Parameter 'table' is required"));
        }

        // Validate table name to prevent SQL injection
        if (!isValidIdentifier(table)) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Invalid table name", "Table name contains invalid characters"));
        }

        try (Connection conn = dataSource.getConnection()) {
            // Determine columns to select
            String selectColumns = "*";
            List<String> columnList = new ArrayList<>();

            if (columns != null && !columns.isBlank()) {
                String[] cols = columns.split(",");
                for (String col : cols) {
                    String trimmed = col.trim();
                    if (!isValidIdentifier(trimmed)) {
                        return ResponseEntity.badRequest()
                            .body(PrintingApiResponse.error("Invalid column name", "Column '" + trimmed + "' contains invalid characters"));
                    }
                    columnList.add(trimmed);
                }
                selectColumns = String.join(", ", columnList);
            }

            // Build query with ORDER BY id for consistent ordering
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(selectColumns).append(" FROM ").append(escapeIdentifier(table));
            sql.append(" ORDER BY id ASC");

            if (limit != null && limit > 0) {
                sql.append(" LIMIT ").append(limit);
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql.toString())) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Get column names from result set
                List<String> resultColumns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    resultColumns.add(metaData.getColumnName(i));
                }

                // Fetch data
                List<Map<String, Object>> data = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(colName, value);
                    }
                    data.add(row);
                }

                PrintingApiResponse response = PrintingApiResponse.success();
                response.setDatabase(database);
                response.setTable(table);
                response.setColumnNames(resultColumns);
                response.setRowCount(data.size());
                response.setData(data);
                return ResponseEntity.ok(response);
            }
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                .body(PrintingApiResponse.error("Database error", e.getMessage()));
        }
    }

    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        // Allow only alphanumeric characters and underscores
        return identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    private String escapeIdentifier(String identifier) {
        // Double-quote the identifier for PostgreSQL
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}

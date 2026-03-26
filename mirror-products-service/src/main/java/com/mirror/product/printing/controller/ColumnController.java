package com.mirror.product.printing.controller;

import com.mirror.product.printing.dto.PrintingApiResponse;
import com.mirror.product.printing.dto.ColumnInfo;
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
public class ColumnController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/columns")
    public ResponseEntity<PrintingApiResponse> listColumns(
            @RequestParam("db") String database,
            @RequestParam("table") String table) {

        if (database == null || database.isBlank()) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Missing parameter", "Parameter 'db' is required"));
        }
        if (table == null || table.isBlank()) {
            return ResponseEntity.badRequest()
                .body(PrintingApiResponse.error("Missing parameter", "Parameter 'table' is required"));
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT column_name, data_type, is_nullable = 'YES' as nullable " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = ? " +
                        "ORDER BY ordinal_position";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, table);

                try (ResultSet rs = stmt.executeQuery()) {
                    List<ColumnInfo> columns = new ArrayList<>();
                    while (rs.next()) {
                        columns.add(new ColumnInfo(
                            rs.getString("column_name"),
                            rs.getString("data_type"),
                            rs.getBoolean("nullable")
                        ));
                    }

                    PrintingApiResponse response = PrintingApiResponse.success();
                    response.setDatabase(database);
                    response.setTable(table);
                    response.setColumns(columns);
                    return ResponseEntity.ok(response);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(PrintingApiResponse.error("Database error", e.getMessage()));
        }
    }
}

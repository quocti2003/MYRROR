package com.mirror.product.printing.controller;

import com.mirror.product.printing.dto.PrintingApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/printing")
public class DatabaseController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/databases")
    public ResponseEntity<PrintingApiResponse> listDatabases() {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT datname FROM pg_database WHERE datistemplate = false AND datname != 'rdsadmin' ORDER BY datname";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<String> databases = new ArrayList<>();
                while (rs.next()) {
                    databases.add(rs.getString("datname"));
                }

                PrintingApiResponse response = PrintingApiResponse.success();
                response.setDatabases(databases);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(PrintingApiResponse.error("Database error", e.getMessage()));
        }
    }
}

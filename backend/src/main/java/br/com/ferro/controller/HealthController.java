package br.com.ferro.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "online");

        try {
            Map<String, Object> database = jdbc.queryForMap(
                    "SELECT DATABASE() AS database_name, VERSION() AS version"
            );

            jdbc.queryForObject("SELECT 1", Integer.class);

            response.put("banco", database.get("database_name"));
            response.put("servidor", database.get("version"));
            response.put("databaseConnection", "ok");
            response.put("status", "ok");
            return ResponseEntity.ok(response);
        } catch (DataAccessException e) {
            response.put("databaseConnection", "erro");
            response.put("status", "database_error");
            response.put("message", databaseMessage(e));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    private String databaseMessage(DataAccessException e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return "Não foi possível conectar ao MariaDB.";
        return message;
    }
}

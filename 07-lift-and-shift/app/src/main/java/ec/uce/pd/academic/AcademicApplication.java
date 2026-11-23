package ec.uce.pd.academic;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The "legacy" system to migrate: a small academic API backed by PostgreSQL. */
@SpringBootApplication
@RestController
public class AcademicApplication {

    private static final Logger log = LoggerFactory.getLogger(AcademicApplication.class);

    private final JdbcTemplate jdbc;
    private final String databaseUrl;

    public AcademicApplication(JdbcTemplate jdbc, @Value("${spring.datasource.url}") String databaseUrl) {
        this.jdbc = jdbc;
        this.databaseUrl = databaseUrl;
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademicApplication.class, args);
    }

    record NewStudent(String name) {}

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== ACADEMIC APP STARTED on host {} | database: {} =====", hostname(), databaseUrl);
    }

    @GetMapping("/health")
    Map<String, String> health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "ok");
    }

    // Where the application is running and what it is connected to: used to compare before and after the migration.
    @GetMapping("/info")
    Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("host", hostname());
        info.put("database", databaseUrl);
        info.put("java", System.getProperty("java.version"));
        info.put("students", jdbc.queryForObject("SELECT count(*) FROM students", Integer.class));
        return info;
    }

    @GetMapping("/students")
    List<Map<String, Object>> students() {
        return jdbc.queryForList("SELECT id, name FROM students ORDER BY id");
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@RequestBody NewStudent student) {
        Integer id = jdbc.queryForObject("INSERT INTO students (name) VALUES (?) RETURNING id", Integer.class, student.name());
        log.info("student {} created: {}", id, student.name());
        return Map.of("id", id, "name", student.name());
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

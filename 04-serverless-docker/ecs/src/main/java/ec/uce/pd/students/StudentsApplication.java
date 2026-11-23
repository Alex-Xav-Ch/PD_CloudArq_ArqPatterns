package ec.uce.pd.students;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class StudentsApplication {

    private static final Logger log = LoggerFactory.getLogger(StudentsApplication.class);

    private static final Map<String, String> STUDENTS = Map.of(
            "1", "Ana Torres",
            "2", "Luis Mena");

    public static void main(String[] args) {
        SpringApplication.run(StudentsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== STUDENTS API (container) STARTED on port 8080 =====");
    }

    @GetMapping("/students/{id}")
    ResponseEntity<Map<String, Object>> student(@PathVariable String id) {
        String name = STUDENTS.get(id);
        log.info("request for student {} -> {}", id, name == null ? 404 : 200);
        if (name == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "student not found"));
        }
        return ResponseEntity.ok(Map.of("id", id, "name", name, "runtime", "ecs-fargate"));
    }

    // Used by the load balancer health check.
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }
}

package ec.uce.pd.enrollment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class EnrollmentApplication {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentApplication.class);

    private final Map<String, Integer> requests = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== ENROLLMENT SERVICE STARTED on port 8002 =====");
    }

    /** Answers 503 to the first `failTimes` requests of each student and 200 afterwards. "ghost" does not exist (404). */
    @GetMapping("/enrollment/{studentId}")
    ResponseEntity<Map<String, Object>> enrollment(@PathVariable String studentId,
                                                   @RequestParam(defaultValue = "2") int failTimes) {
        if ("ghost".equals(studentId)) {
            log.warn("request for {} -> 404 (student does not exist)", studentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "student not found"));
        }
        int n = requests.merge(studentId, 1, Integer::sum);
        if (n <= failTimes) {
            log.warn("request {} for {} -> 503 (temporarily unavailable)", n, studentId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("detail", "temporarily unavailable"));
        }
        log.info("request {} for {} -> 200 (ENROLLED)", n, studentId);
        return ResponseEntity.ok(Map.of("studentId", studentId, "status", "ENROLLED"));
    }

    // Lets the demo start every run from a clean state.
    @PostMapping("/reset")
    Map<String, Boolean> reset() {
        requests.clear();
        log.info("===== ENROLLMENT SERVICE RESET (request counters cleared) =====");
        return Map.of("reset", true);
    }
}

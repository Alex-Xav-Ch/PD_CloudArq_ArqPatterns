package ec.uce.pd.client;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import jakarta.annotation.PostConstruct;

@RestController
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final EnrollmentGateway gateway;
    private final RetryRegistry registry;

    public StudentController(EnrollmentGateway gateway, RetryRegistry registry) {
        this.gateway = gateway;
        this.registry = registry;
    }

    // Shows the pause that Resilience4j applies between attempts.
    @PostConstruct
    void logRetries() {
        registry.retry("enrollment").getEventPublisher()
                .onRetry(e -> log.warn("  >>> RETRY #{}: waiting {} ms before the next attempt",
                        e.getNumberOfRetryAttempts(), e.getWaitInterval().toMillis()));
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== STUDENT CLIENT STARTED on port 8003 (max 3 attempts, exponential backoff) =====");
    }

    // Called by Demo.java so the log shows where each scenario begins.
    @PostMapping("/demo/phase")
    void phase(@RequestParam String name) {
        log.info("");
        log.info("########## PHASE: {} ##########", name);
    }

    @GetMapping("/students/{id}/enrollment")
    Map<String, Object> enrollment(@PathVariable String id, @RequestParam(defaultValue = "2") int failTimes) {
        var attempts = new AtomicInteger();
        log.info("request for student {} received", id);
        try {
            var result = gateway.fetch(id, failTimes, attempts);
            log.info("student {} RESOLVED after {} attempt(s)", id, attempts.get());
            return Map.of("studentId", id, "enrollment", result, "attempts", attempts.get());
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            log.error("student {} FAILED: gave up after {} attempts (503)", id, attempts.get());
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("student {} NOT FOUND (404): not retried, attempts: {}", id, attempts.get());
            throw e;
        }
    }

    @ExceptionHandler(HttpServerErrorException.ServiceUnavailable.class)
    ResponseEntity<Map<String, String>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("detail", "enrollment service unavailable after 3 attempts"));
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "student not found"));
    }
}

package ec.uce.pd.client;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@RestController
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final RestClient client;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public StudentController(RestClient enrollmentClient,
                             @Value("${enrollment.connect-timeout-ms}") int connectTimeoutMs,
                             @Value("${enrollment.read-timeout-ms}") int readTimeoutMs) {
        this.client = enrollmentClient;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== STUDENT CLIENT STARTED on port 8005 (connect timeout {} ms, read timeout {} ms) =====",
                connectTimeoutMs, readTimeoutMs);
    }

    // Called by Demo.java so the log shows where each scenario begins.
    @PostMapping("/demo/phase")
    void phase(@RequestParam String name) {
        log.info("");
        log.info("########## PHASE: {} ##########", name);
    }

    @GetMapping("/students/{id}/enrollment")
    @SuppressWarnings("unchecked")
    ResponseEntity<Map<String, Object>> enrollment(@PathVariable String id,
                                                   @RequestParam(defaultValue = "0") double delay) {
        log.info("request for student {} sent: service will take {} ms, read timeout is {} ms",
                id, Math.round(delay * 1000), readTimeoutMs);
        long start = System.nanoTime();
        try {
            var body = client.get().uri("/enrollment/{id}?delay={delay}", id, delay).retrieve().body(Map.class);
            log.info("student {} response received in {} ms -> OK", id, elapsedMs(start));
            return ResponseEntity.ok(body);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException timeout) {
                log.error("student {} TIMEOUT after {} ms (limit {} ms): {}", id, elapsedMs(start), readTimeoutMs, timeout.getMessage());
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                        .body(Map.of("detail", "enrollment service did not answer within " + readTimeoutMs + " ms"));
            }
            log.error("student {} connection error after {} ms: {}", id, elapsedMs(start), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("detail", "enrollment service unreachable"));
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}

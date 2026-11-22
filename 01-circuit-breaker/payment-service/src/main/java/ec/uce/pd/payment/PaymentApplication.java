package ec.uce.pd.payment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class PaymentApplication {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplication.class);

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }

    record Payment(long orderId) {}

    record Health(boolean healthy) {}

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== PAYMENT SERVICE STARTED on port 8001 (status: UP) =====");
    }

    @PostMapping("/pay")
    ResponseEntity<Map<String, Object>> pay(@RequestBody Payment payment) {
        if (!healthy.get()) {
            log.warn("payment for order {} -> FAILED (500), service is DOWN", payment.orderId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("detail", "payment gateway down"));
        }
        log.info("payment for order {} -> PAID", payment.orderId());
        return ResponseEntity.ok(Map.of("orderId", payment.orderId(), "status", "PAID"));
    }

    // Test hook: lets the demo break and repair this service on demand.
    @PostMapping("/admin/health")
    Map<String, Boolean> setHealth(@RequestBody Health body) {
        healthy.set(body.healthy());
        log.info("===== PAYMENT SERVICE SET TO {} =====", body.healthy() ? "UP (recovered)" : "DOWN (simulated failure)");
        return Map.of("healthy", healthy.get());
    }
}

package ec.uce.pd.order;

import java.util.Map;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final PaymentGateway payments;
    private final CircuitBreakerRegistry registry;

    public OrderController(PaymentGateway payments, CircuitBreakerRegistry registry) {
        this.payments = payments;
        this.registry = registry;
    }

    // Logs every state transition: this is the main evidence of the pattern.
    @PostConstruct
    void logTransitions() {
        var breaker = registry.circuitBreaker("payment");
        breaker.getEventPublisher()
                .onStateTransition(e -> log.info(">>> CIRCUIT STATE CHANGE: {} -> {}",
                        e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                .onError(e -> log.warn("    payment call failed ({})", e.getThrowable().getClass().getSimpleName()));
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== ORDER SERVICE STARTED on port 8000 (circuit: {}) =====", state());
    }

    // Called by Demo.java so the log shows where each phase of the demo begins.
    @PostMapping("/demo/phase")
    void phase(@RequestParam String name) {
        log.info("");
        log.info("########## PHASE: {} ##########", name);
    }

    @PostMapping("/orders/{id}")
    Map<String, Object> createOrder(@PathVariable long id) {
        log.info("order {} received (circuit: {})", id, state());
        Map<String, Object> payment;
        try {
            payment = payments.pay(id);
        } catch (CallNotPermittedException e) {
            log.warn("order {} REJECTED without calling payment: circuit is OPEN (503)", id);
            throw e;
        } catch (RestClientException e) {
            log.error("order {} FAILED: payment error (502): {}", id, e.getMessage());
            throw e;
        }
        log.info("order {} COMPLETED, payment accepted (circuit: {})", id, state());
        return Map.of("orderId", id, "payment", payment, "breaker", state());
    }

    @GetMapping("/breaker")
    Map<String, String> breaker() {
        return Map.of("state", state());
    }

    private String state() {
        return registry.circuitBreaker("payment").getState().name();
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ResponseEntity<Map<String, String>> circuitOpen(CallNotPermittedException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("detail", "payment unavailable (circuit open)"));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<Map<String, String>> paymentFailed(RestClientException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("detail", "payment failed"));
    }
}

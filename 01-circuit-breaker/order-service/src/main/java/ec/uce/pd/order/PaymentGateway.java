package ec.uce.pd.order;

import java.util.Map;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentGateway {

    private final RestClient client;

    public PaymentGateway(RestClient paymentClient) {
        this.client = paymentClient;
    }

    // Every failure (HTTP 5xx, timeout, connection error) counts towards opening the circuit.
    @CircuitBreaker(name = "payment")
    @SuppressWarnings("unchecked")
    public Map<String, Object> pay(long orderId) {
        return client.post()
                .body(Map.of("orderId", orderId))
                .retrieve()
                .body(Map.class);
    }
}

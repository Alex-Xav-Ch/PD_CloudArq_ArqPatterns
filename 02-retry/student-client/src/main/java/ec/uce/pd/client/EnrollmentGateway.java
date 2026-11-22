package ec.uce.pd.client;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EnrollmentGateway {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentGateway.class);

    private final RestClient client;

    public EnrollmentGateway(RestClient enrollmentClient) {
        this.client = enrollmentClient;
    }

    // Every call of this method is one attempt; Resilience4j re-invokes it according to the "enrollment" policy.
    // `attempts` is only used to number the attempts in the logs and in the response.
    @Retry(name = "enrollment")
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch(String studentId, int failTimes, AtomicInteger attempts) {
        int n = attempts.incrementAndGet();
        log.info("  attempt {} -> GET /enrollment/{}", n, studentId);
        try {
            var body = client.get()
                    .uri("/enrollment/{id}?failTimes={failTimes}", studentId, failTimes)
                    .retrieve()
                    .body(Map.class);
            log.info("  attempt {} <- HTTP 200", n);
            return body;
        } catch (RestClientResponseException e) {
            log.warn("  attempt {} <- HTTP {}", n, e.getStatusCode().value());
            throw e;
        }
    }
}

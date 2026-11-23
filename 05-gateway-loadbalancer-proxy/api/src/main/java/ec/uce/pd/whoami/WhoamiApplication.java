package ec.uce.pd.whoami;

import java.net.InetAddress;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class WhoamiApplication {

    private static final Logger log = LoggerFactory.getLogger(WhoamiApplication.class);

    // Every Fargate task is a different container, so its hostname identifies the instance.
    private final String instance = hostname();

    public static void main(String[] args) {
        SpringApplication.run(WhoamiApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== WHOAMI API STARTED on port 8080, instance: {} =====", instance);
    }

    @GetMapping("/whoami")
    Map<String, String> whoami() {
        log.info("request handled by instance {}", instance);
        return Map.of("instance", instance);
    }

    // Used by the load balancer health check.
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

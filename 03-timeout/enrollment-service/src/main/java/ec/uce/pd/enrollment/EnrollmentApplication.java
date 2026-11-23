package ec.uce.pd.enrollment;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class EnrollmentApplication {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void started() {
        log.info("===== ENROLLMENT SERVICE STARTED on port 8004 =====");
    }

    /** Waits `delay` seconds before answering, to simulate a slow dependency. */
    @GetMapping("/enrollment/{studentId}")
    Map<String, Object> enrollment(@PathVariable String studentId,
                                   @RequestParam(defaultValue = "0") double delay) throws InterruptedException {
        long ms = Math.round(delay * 1000);
        log.info("request for {} received: will answer in {} ms", studentId, ms);
        Thread.sleep(ms);
        log.info("response for {} sent after {} ms (the client may have given up already)", studentId, ms);
        return Map.of("studentId", studentId, "status", "ENROLLED", "delayMs", ms);
    }
}

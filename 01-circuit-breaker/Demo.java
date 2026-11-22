import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

/** Drives both services through CLOSED -> OPEN -> HALF_OPEN -> CLOSED. Run: java Demo.java */
public class Demo {

    static final String ORDER = "http://127.0.0.1:8000";
    static final String PAYMENT = "http://127.0.0.1:8001";
    static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        phase("1. Normal traffic (circuit CLOSED)");
        placeOrder(1);

        phase("2. Payment service fails: 3 failures open the circuit");
        setPaymentHealth(false);
        for (int i = 2; i <= 4; i++) placeOrder(i);

        phase("3. Calls are rejected immediately while the circuit is OPEN");
        placeOrder(5);
        placeOrder(6);

        phase("4. Payment recovers, waiting 10 s for HALF_OPEN");
        setPaymentHealth(true);
        Thread.sleep(10_500);
        System.out.println("breaker=" + get(ORDER + "/breaker"));

        phase("5. Probe request succeeds, circuit closes");
        placeOrder(7);
        placeOrder(8);

        phase("DEMO FINISHED");
    }

    // Prints the phase and also writes it to the order service log.
    static void phase(String name) throws Exception {
        System.out.println("== " + name);
        var url = ORDER + "/demo/phase?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        http.send(HttpRequest.newBuilder(URI.create(url)).POST(BodyPublishers.noBody()).build(), BodyHandlers.ofString());
    }

    static void placeOrder(int id) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(ORDER + "/orders/" + id))
                .POST(BodyPublishers.noBody()).build();
        int status = http.send(request, BodyHandlers.ofString()).statusCode();
        System.out.println("order " + id + ": HTTP " + status + " | breaker=" + get(ORDER + "/breaker"));
    }

    static void setPaymentHealth(boolean healthy) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(PAYMENT + "/admin/health"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\"healthy\":" + healthy + "}")).build();
        http.send(request, BodyHandlers.ofString());
        System.out.println("--- payment service healthy=" + healthy);
    }

    static String get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).build(), BodyHandlers.ofString()).body();
    }
}

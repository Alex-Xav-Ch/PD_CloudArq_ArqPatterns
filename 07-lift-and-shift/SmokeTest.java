import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;

/**
 * Functional check run against the origin (before) and the target (after) of the migration.
 * Run: java SmokeTest.java <base_url>
 */
public class SmokeTest {

    static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: java SmokeTest.java <base_url>");
            return;
        }
        String base = args[0].replaceAll("/$", "");
        System.out.println("target: " + base);

        show("GET  /info", get(base + "/info"));
        show("GET  /health", get(base + "/health"));
        show("GET  /students", get(base + "/students"));

        String body = "{\"name\":\"Smoke test " + LocalDateTime.now().withNano(0) + "\"}";
        var post = HttpRequest.newBuilder(URI.create(base + "/students"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body)).build();
        show("POST /students", http.send(post, BodyHandlers.ofString()));
    }

    static HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).build(), BodyHandlers.ofString());
    }

    static void show(String label, HttpResponse<String> r) {
        System.out.println(label + " -> HTTP " + r.statusCode() + " | " + r.body());
    }
}

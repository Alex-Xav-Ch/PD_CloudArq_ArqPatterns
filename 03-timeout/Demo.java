import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

/** Queries enrollments with different service delays. Run: java Demo.java */
public class Demo {

    static final String CLIENT = "http://127.0.0.1:8005";
    static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        phase("1. Fast response (0.5 s): well inside the 2 s limit");
        request("alice", 0.5);

        phase("2. Slower response (1.5 s): still inside the 2 s limit");
        request("bob", 1.5);

        phase("3. Very slow response (5 s): the 2 s limit is exceeded");
        request("carol", 5.0);

        phase("DEMO FINISHED");
    }

    // Prints the phase and also writes it to the student client log.
    static void phase(String name) throws Exception {
        System.out.println("== " + name);
        var url = CLIENT + "/demo/phase?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        http.send(HttpRequest.newBuilder(URI.create(url)).POST(BodyPublishers.noBody()).build(), BodyHandlers.ofString());
    }

    static void request(String student, double delaySeconds) throws Exception {
        long start = System.nanoTime();
        var response = http.send(
                HttpRequest.newBuilder(URI.create(CLIENT + "/students/" + student + "/enrollment?delay=" + delaySeconds)).build(),
                BodyHandlers.ofString());
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println(student + " (delay=" + delaySeconds + "s): HTTP " + response.statusCode() + " in " + ms + " ms | " + response.body());
    }
}

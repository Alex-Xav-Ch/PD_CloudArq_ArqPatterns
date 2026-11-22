import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

/** Runs the retry scenarios against the student client. Run: java Demo.java */
public class Demo {

    static final String CLIENT = "http://127.0.0.1:8003";
    static final String ENROLLMENT = "http://127.0.0.1:8002";
    static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        post(ENROLLMENT + "/reset");

        phase("1. Two transient failures, then success (recovers on attempt 3)");
        request("alice", 2);

        phase("2. Every attempt fails (gives up after 3 attempts)");
        request("bob", 99);

        phase("3. Non-transient error: 404 is not retried");
        request("ghost", 2);

        phase("DEMO FINISHED");
    }

    // Prints the phase and also writes it to the student client log.
    static void phase(String name) throws Exception {
        System.out.println("== " + name);
        post(CLIENT + "/demo/phase?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
    }

    static void request(String student, int failTimes) throws Exception {
        long start = System.nanoTime();
        var response = http.send(
                HttpRequest.newBuilder(URI.create(CLIENT + "/students/" + student + "/enrollment?failTimes=" + failTimes)).build(),
                BodyHandlers.ofString());
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println(student + ": HTTP " + response.statusCode() + " in " + ms + " ms | " + response.body());
    }

    static HttpResponse<String> post(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).POST(BodyPublishers.noBody()).build(), BodyHandlers.ofString());
    }
}

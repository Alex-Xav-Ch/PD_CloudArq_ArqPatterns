import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows the role of each component. Run: java Demo.java <api_gateway_url> <alb_url>
 * 1. Load balancing: sequential requests through the gateway are spread between the two tasks.
 * 2. Throttling: a burst through the gateway is partly rejected with 429.
 * 3. Same burst straight to the ALB: nothing is rejected, the ALB does not limit the rate.
 */
public class Demo {

    static final int BURST = 100;
    static final HttpClient http = HttpClient.newHttpClient();
    static final Pattern INSTANCE = Pattern.compile("\"instance\":\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: java Demo.java <api_gateway_url> <alb_url>");
            return;
        }
        String gateway = args[0] + "/whoami";
        String alb = args[1] + "/whoami";

        System.out.println("== 1. Load balancing: 12 sequential requests through the API Gateway");
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(gateway)).build(), BodyHandlers.ofString());
            String instance = instanceOf(r.body());
            counts.merge(instance, 1, Integer::sum);
            System.out.println("request " + i + ": HTTP " + r.statusCode() + " | instance " + instance);
            Thread.sleep(700); // slow enough to stay under the gateway limit (2 requests/s)
        }
        System.out.println("summary: " + counts);

        Thread.sleep(2000);
        System.out.println("== 2. Throttling: burst of " + BURST + " simultaneous requests through the API Gateway");
        burst(gateway, BURST);

        Thread.sleep(2000);
        System.out.println("== 3. Same burst of " + BURST + " requests straight to the ALB (no gateway)");
        burst(alb, BURST);

        System.out.println("== DEMO FINISHED");
    }

    static void burst(String url, int n) {
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(http.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), BodyHandlers.ofString()));
        }
        Map<Integer, Integer> statuses = new TreeMap<>();
        Map<String, Integer> instances = new TreeMap<>();
        for (var f : futures) {
            HttpResponse<String> r = f.join();
            statuses.merge(r.statusCode(), 1, Integer::sum);
            if (r.statusCode() == 200) {
                instances.merge(instanceOf(r.body()), 1, Integer::sum);
            }
        }
        System.out.println("status codes: " + statuses);
        System.out.println("answered by: " + instances);
    }

    static String instanceOf(String body) {
        Matcher m = INSTANCE.matcher(body);
        return m.find() ? m.group(1) : "?";
    }
}

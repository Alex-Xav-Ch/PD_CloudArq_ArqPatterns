import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends the same requests to the Lambda and to the ECS/Fargate versions and compares response times.
 * Run: java Demo.java <lambda_api_url> <ecs_alb_url>
 */
public class Demo {

    static final int REQUESTS = 10;
    static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: java Demo.java <lambda_api_url> <ecs_alb_url>");
            return;
        }
        compare("AWS Lambda (API Gateway)", args[0]);
        compare("Amazon ECS on Fargate (ALB)", args[1]);
        System.out.println("== DEMO FINISHED");
    }

    static void compare(String label, String baseUrl) throws Exception {
        System.out.println("== " + label + " | " + baseUrl);
        List<Long> times = new ArrayList<>();
        for (int i = 1; i <= REQUESTS; i++) {
            long start = System.nanoTime();
            HttpResponse<String> response = get(baseUrl + "/students/1");
            long ms = (System.nanoTime() - start) / 1_000_000;
            times.add(ms);
            System.out.println("request " + i + ": HTTP " + response.statusCode() + " in " + ms + " ms | " + response.body());
        }
        HttpResponse<String> missing = get(baseUrl + "/students/99");
        System.out.println("student 99: HTTP " + missing.statusCode() + " | " + missing.body());

        long first = times.get(0);
        double rest = times.subList(1, times.size()).stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf("summary: first request %d ms | average of the next %d requests %.0f ms%n", first, REQUESTS - 1, rest);
    }

    static HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).build(), BodyHandlers.ofString());
    }
}

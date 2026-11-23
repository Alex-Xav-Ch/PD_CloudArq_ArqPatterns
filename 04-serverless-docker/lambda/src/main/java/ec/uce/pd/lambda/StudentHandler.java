package ec.uce.pd.lambda;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

/** GET /students/{id} behind an API Gateway HTTP API (payload format 2.0). */
public class StudentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Map<String, String> STUDENTS = Map.of(
            "1", "Ana Torres",
            "2", "Luis Mena");

    // Static state lives as long as the execution environment: true only on the first call (cold start).
    private static boolean coldStart = true;

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        boolean wasCold = coldStart;
        coldStart = false;

        var pathParameters = event.getPathParameters();
        String id = pathParameters == null ? null : pathParameters.get("id");
        String name = STUDENTS.get(id);

        context.getLogger().log("request for student " + id + " | cold start: " + wasCold
                + " | " + (name == null ? "404" : "200") + "\n");

        if (name == null) {
            return response(404, "{\"detail\":\"student not found\"}");
        }
        return response(200, "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"runtime\":\"lambda\",\"coldStart\":" + wasCold + "}");
    }

    private static APIGatewayV2HTTPResponse response(int status, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body)
                .build();
    }
}

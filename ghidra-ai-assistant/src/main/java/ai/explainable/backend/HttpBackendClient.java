package ai.explainable.backend;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HttpBackendClient implements BackendClient {
    private final String baseUrl;
    private final Gson gson = new Gson();
    private final HttpClient client;

    public HttpBackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Override
    public <T> T post(String endpoint, Object requestBody, Class<T> responseType) throws Exception {
        String jsonBody = gson.toJson(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Backend error (" + response.statusCode() + "): " + response.body());
        }
        return gson.fromJson(response.body(), responseType);
    }

    @Override
    public <TReq, TRes> TRes analyze(String componentId, TReq requestBody, Class<TRes> responseType) throws Exception {
        return post("/analyze/" + componentId, requestBody, responseType);
    }
}

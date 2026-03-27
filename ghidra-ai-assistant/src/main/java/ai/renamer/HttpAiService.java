package ai.renamer;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpAiService implements AiService {
    private final String baseUrl;
    private final Gson gson = new Gson();

    public HttpAiService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public RenameResult performRename(String decompiledCode, String currentFunctionName) {
        return callBackend("/rename", decompiledCode, currentFunctionName, RenameResult.class);
    }

    @Override
    public MemorySafetyResult performMemorySafetyAnalysis(String decompiledCode, String currentFunctionName) {
        return callBackend("/memory_safety", decompiledCode, currentFunctionName, MemorySafetyResult.class);
    }

    private <T> T callBackend(String endpoint, String decompiledCode, String currentFunctionName, Class<T> clazz) {
        try {
            String jsonBody = gson.toJson(new RenameRequest(decompiledCode, currentFunctionName));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), clazz);
            } else {
                throw new RuntimeException("Backend error (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call AI backend: " + e.getMessage(), e);
        }
    }

    private static class RenameRequest {
        String decompiled_code;
        String function_name;
        RenameRequest(String decompiled_code, String function_name) {
            this.decompiled_code = decompiled_code;
            this.function_name = function_name;
        }
    }
}
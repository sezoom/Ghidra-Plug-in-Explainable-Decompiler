package ai.explainable.backend;

public interface BackendClient {
    <T> T post(String endpoint, Object requestBody, Class<T> responseType) throws Exception;
}

package ai.explainable.backend;

public interface BackendClient {
    <T> T post(String endpoint, Object requestBody, Class<T> responseType) throws Exception;

    <TReq, TRes> TRes analyze(String componentId, TReq requestBody, Class<TRes> responseType) throws Exception;
}

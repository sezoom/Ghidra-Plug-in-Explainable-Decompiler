package ai.explainable.components;

import ai.explainable.backend.BackendClient;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

public interface AnalysisComponent<R> {
    String getId();
    String getDisplayName();
    Class<R> getResultType();
    R analyze(AnalysisContext context, BackendClient client) throws Exception;
    void renderResult(AnalysisContext context, R result, AnalysisView view);

    default boolean supportsApply(AnalysisContext context, R result) {
        return false;
    }

    default void apply(AnalysisContext context, R result, AnalysisView view) throws Exception {
        throw new UnsupportedOperationException(getDisplayName() + " does not support apply.");
    }
}

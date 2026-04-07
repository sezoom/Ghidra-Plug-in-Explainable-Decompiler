package ai.explainable.components.deobfuscation;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

public class DeobfuscationComponent implements AnalysisComponent<DeobfuscationResult> {
    @Override
    public String getId() { return "deobfuscation"; }
    @Override
    public String getDisplayName() { return "Deobfuscation/Simplify"; }
    @Override
    public Class<DeobfuscationResult> getResultType() { return DeobfuscationResult.class; }
    @Override
    public DeobfuscationResult analyze(AnalysisContext context, BackendClient client) throws Exception {
        DeobfuscationRequest request = new DeobfuscationRequest(context.getDecompiledCode(), context.getFunction().getName());
        return client.analyze(getId(), request, DeobfuscationResult.class);
    }
    @Override
    public void renderResult(AnalysisContext context, DeobfuscationResult result, AnalysisView view) {
        view.showCodePreview(context.getDecompiledCode(), java.util.Collections.emptyList());
        view.showResultText(result == null ? "No deobfuscation result returned." : AnalysisContext.safeTrim(result.getSummary()));
        view.setApplyEnabled(false);
    }
}

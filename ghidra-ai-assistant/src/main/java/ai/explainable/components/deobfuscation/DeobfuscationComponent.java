package ai.explainable.components.deobfuscation;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

import java.util.Collections;

public class DeobfuscationComponent implements AnalysisComponent<DeobfuscationResult> {
    @Override
    public String getId() {
        return "deobfuscation";
    }

    @Override
    public String getDisplayName() {
        return "Deobfuscation / Simplify";
    }

    @Override
    public Class<DeobfuscationResult> getResultType() {
        return DeobfuscationResult.class;
    }

    @Override
    public DeobfuscationResult analyze(AnalysisContext context, BackendClient client) throws Exception {
        DeobfuscationRequest request =
            new DeobfuscationRequest(context.getDecompiledCode(), context.getFunction().getName());
        return client.analyze(getId(), request, DeobfuscationResult.class);
    }

    @Override
    public void renderResult(AnalysisContext context, DeobfuscationResult result, AnalysisView view) {
        if (result == null) {
            view.showCodePreview(context.getDecompiledCode(), Collections.emptyList());
            view.showResultText("No deobfuscation result returned.");
            view.setApplyEnabled(false);
            return;
        }

        String cleanedCode = result.hasCleanCode()
            ? result.getCleanCode().trim()
            : context.getDecompiledCode();

        view.showCodePreview(cleanedCode, Collections.emptyList());
        view.showResultText(format(result));
        view.setApplyEnabled(false);
    }

    private String format(DeobfuscationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal:\n")
          .append("- Simplify Ghidra decompiled code into a human-friendly, clean programming style.\n")
          .append("- Deobfuscate obfuscated constructs into a clearer, human-friendly form.\n\n");

        if (result.hasCleanCode()) {
            sb.append("Output:\n")
              .append("Cleaned/deobfuscated code is shown in the preview panel above.\n\n");
        }
        else {
            sb.append("Output:\n")
              .append("No rewritten code was returned, so the original decompiled code remains in the preview panel.\n\n");
        }

        sb.append("Summary:\n")
          .append(AnalysisContext.safeTrim(result.getSummary()));

        String changes = AnalysisContext.safeTrim(result.getChangesSummary());
        if (!changes.isEmpty()) {
            sb.append("\n\nTransformation Notes:\n")
              .append(changes);
        }

        return sb.toString();
    }
}

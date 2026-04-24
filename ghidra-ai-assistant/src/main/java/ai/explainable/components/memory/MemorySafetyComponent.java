package ai.explainable.components.memory;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

public class MemorySafetyComponent
    implements AnalysisComponent<MemorySafetyResult>
{

    @Override
    public String getId() {
        return "memory_safety";
    }

    @Override
    public String getDisplayName() {
        return "Memory Safety Analysis";
    }

    @Override
    public Class<MemorySafetyResult> getResultType() {
        return MemorySafetyResult.class;
    }

    @Override
    public MemorySafetyResult analyze(
        AnalysisContext context,
        BackendClient client
    ) throws Exception {
        MemorySafetyRequest request = new MemorySafetyRequest(
            context.getDecompiledCode(),
            context.getFunction().getName(),
            context.getSnapshotPath()
        );
        return client.analyze(getId(), request, MemorySafetyResult.class);
    }

    @Override
    public void renderResult(
        AnalysisContext context,
        MemorySafetyResult result,
        AnalysisView view
    ) {
        view.showCodePreview(
            context.getDecompiledCode(),
            java.util.Collections.emptyList()
        );
        view.showResultText(format(result));
        view.setApplyEnabled(false);
    }

    private String format(MemorySafetyResult safetyResult) {
        if (safetyResult == null) {
            return "No memory safety result returned.";
        }
        StringBuilder sb = new StringBuilder();
        sb
            .append("Overall Assessment:\n")
            .append(
                AnalysisContext.safeTrim(safetyResult.getOverallAssessment())
            )
            .append("\n\nIssues:\n");
        if (
            safetyResult.getIssues() == null ||
            safetyResult.getIssues().isEmpty()
        ) {
            sb.append("No memory safety issues detected.");
            return sb.toString();
        }
        for (MemorySafetyIssue issue : safetyResult.getIssues()) {
            sb
                .append("[")
                .append(
                    issue.getSeverity() != null
                        ? issue.getSeverity().toUpperCase()
                        : "UNKNOWN"
                )
                .append("] ")
                .append(issue.getIssueType())
                .append(" @ ")
                .append(issue.getLocation())
                .append("\n")
                .append(issue.getDescription())
                .append("\nSuggestion: ")
                .append(issue.getSuggestion())
                .append("\n\n");
        }

        // ── Append control layer output if present ──────────────────────
        if (
            safetyResult.getControlOutput() != null &&
            !safetyResult.getControlOutput().isBlank()
        ) {
            sb.append(safetyResult.getControlOutput()).append("\n");
        }
        // ─────────────────────────────────────────────────────────────────────

        return sb.toString();
    }
}

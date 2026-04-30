package ai.explainable.components.memory;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemorySafetyComponent
    implements AnalysisComponent<MemorySafetyResult>
{

    private static final List<String> TABLE_COLUMNS = Arrays.asList(
        "ID",
        "Severity",
        "Category",
        "Location",
        "Description",
        "Suggestion"
    );

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
            Collections.emptyList()
        );
        view.showResultTable(
            buildSummary(result),
            TABLE_COLUMNS,
            buildRows(result)
        );
        view.setApplyEnabled(false);
    }

    private String buildSummary(MemorySafetyResult safetyResult) {
        if (safetyResult == null) {
            return "No memory safety result returned.";
        }
        StringBuilder sb = new StringBuilder();
        String summary = AnalysisContext.safeTrim(
            safetyResult.getOverallAssessment()
        );
        sb.append(
            summary.isEmpty() ? "No overall assessment provided." : summary
        );

        // ── Append control layer output if present ───────────────────────
        if (
            safetyResult.getControlOutput() != null &&
            !safetyResult.getControlOutput().isBlank()
        ) {
            sb
                .append("\n")
                .append(safetyResult.getControlOutput())
                .append("\n");
        }
        // ─────────────────────────────────────────────────────────────────

        return sb.toString();
    }

    private List<List<String>> buildRows(MemorySafetyResult safetyResult) {
        if (
            safetyResult == null ||
            safetyResult.getIssues() == null ||
            safetyResult.getIssues().isEmpty()
        ) {
            return Collections.emptyList();
        }

        List<List<String>> rows = new ArrayList<>();
        int index = 1;
        for (MemorySafetyIssue issue : safetyResult.getIssues()) {
            String severity = AnalysisContext.toTitleCaseLabel(issue.getSeverity());
            String category = AnalysisContext.toTitleCaseLabel(issue.getIssueType());
            String location = AnalysisContext.safeTrim(issue.getLocation());
            String description = AnalysisContext.safeTrim(issue.getDescription());
            String suggestion = AnalysisContext.safeTrim(issue.getSuggestion());

            rows.add(
                Arrays.asList(
                    "M" + index,
                    severity,
                    category,
                    location,
                    description,
                    suggestion
                )
            );
            index++;
        }
        return rows;
    }
}

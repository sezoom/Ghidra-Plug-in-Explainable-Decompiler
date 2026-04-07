package ai.explainable.components.crypto;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CryptoAnalysisComponent implements AnalysisComponent<CryptoAnalysisResult> {
    private static final List<String> TABLE_COLUMNS = Arrays.asList(
        "ID",
        "Severity",
        "Category",
        "Function / Location",
        "Technical Finding",
        "Security Impact",
        "Mitigation"
    );

    @Override
    public String getId() {
        return "crypto";
    }

    @Override
    public String getDisplayName() {
        return "Crypto Analysis";
    }

    @Override
    public Class<CryptoAnalysisResult> getResultType() {
        return CryptoAnalysisResult.class;
    }

    @Override
    public CryptoAnalysisResult analyze(AnalysisContext context, BackendClient client) throws Exception {
        CryptoAnalysisRequest request =
            new CryptoAnalysisRequest(context.getDecompiledCode(), context.getFunction().getName());
        return client.analyze(getId(), request, CryptoAnalysisResult.class);
    }

    @Override
    public void renderResult(AnalysisContext context, CryptoAnalysisResult result, AnalysisView view) {
        view.showCodePreview(context.getDecompiledCode(), Collections.emptyList());
        view.showResultTable(
            buildSummary(result),
            TABLE_COLUMNS,
            buildRows(result)
        );
        view.setApplyEnabled(false);
    }

    private String buildSummary(CryptoAnalysisResult cryptoResult) {
        if (cryptoResult == null) {
            return "No crypto analysis result returned.";
        }
        String summary = AnalysisContext.safeTrim(cryptoResult.getOverallAssessment());
        return summary.isEmpty() ? "No overall assessment provided." : summary;
    }

    private List<List<String>> buildRows(CryptoAnalysisResult cryptoResult) {
        if (cryptoResult == null || cryptoResult.getIssues() == null || cryptoResult.getIssues().isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> rows = new ArrayList<>();
        int index = 1;
        for (CryptoAnalysisIssue issue : cryptoResult.getIssues()) {
            String severity = AnalysisContext.toTitleCaseLabel(issue.getSeverity());
            String category = AnalysisContext.toTitleCaseLabel(issue.getIssueType());
            String location = AnalysisContext.safeTrim(issue.getLocation());
            String finding = AnalysisContext.safeTrim(issue.getDescription());
            String impact = AnalysisContext.deriveCryptoSecurityImpact(issue.getIssueType(), issue.getSeverity());
            String mitigation = AnalysisContext.safeTrim(issue.getSuggestion());

            rows.add(Arrays.asList(
                "C" + index,
                severity,
                category,
                location,
                finding,
                impact,
                mitigation
            ));
            index++;
        }
        return rows;
    }
}

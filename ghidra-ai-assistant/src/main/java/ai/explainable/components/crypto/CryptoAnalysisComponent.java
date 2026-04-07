package ai.explainable.components.crypto;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;

public class CryptoAnalysisComponent implements AnalysisComponent<CryptoAnalysisResult> {
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
        view.showCodePreview(context.getDecompiledCode(), java.util.Collections.emptyList());
        view.showResultText(format(result));
        view.setApplyEnabled(false);
    }

    private String format(CryptoAnalysisResult cryptoResult) {
        if (cryptoResult == null) {
            return "No crypto analysis result returned.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Overall Assessment:\n")
          .append(AnalysisContext.safeTrim(cryptoResult.getOverallAssessment()))
          .append("\n\nIssues:\n");

        if (cryptoResult.getIssues() == null || cryptoResult.getIssues().isEmpty()) {
            sb.append("No cryptographic issues detected.");
            return sb.toString();
        }

        for (CryptoAnalysisIssue issue : cryptoResult.getIssues()) {
            sb.append("[")
              .append(issue.getSeverity() != null ? issue.getSeverity().toUpperCase() : "UNKNOWN")
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

        return sb.toString();
    }
}
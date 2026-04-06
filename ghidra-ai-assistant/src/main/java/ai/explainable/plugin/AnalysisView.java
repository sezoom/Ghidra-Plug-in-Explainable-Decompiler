package ai.explainable.plugin;

import java.util.List;

public interface AnalysisView {
    void showCodePreview(String text, List<HighlightSpan> spans);
    void showResultText(String text);
    void setApplyEnabled(boolean enabled);
    boolean confirm(String title, String message);
    void showInfo(String title, String message);
    void showError(String title, String message, Throwable throwable);

    record HighlightSpan(int start, int end, String tooltip) {}
}

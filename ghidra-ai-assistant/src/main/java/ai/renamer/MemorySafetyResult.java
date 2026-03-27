package ai.renamer;

import java.util.List;

public class MemorySafetyResult {
    private List<MemorySafetyIssue> issues;
    private String overallAssessment;

    public List<MemorySafetyIssue> getIssues() { return issues; }
    public String getOverallAssessment() { return overallAssessment; }
}
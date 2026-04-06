package ai.explainable.components.memory;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MemorySafetyResult {
    @SerializedName("issues")
    private List<MemorySafetyIssue> issues;
    @SerializedName("overall_assessment")
    private String overallAssessment;

    public List<MemorySafetyIssue> getIssues() { return issues; }
    public String getOverallAssessment() { return overallAssessment; }
}

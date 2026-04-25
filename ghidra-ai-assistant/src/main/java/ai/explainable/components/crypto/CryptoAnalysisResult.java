package ai.explainable.components.crypto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CryptoAnalysisResult {

    @SerializedName("issues")
    private List<CryptoAnalysisIssue> issues;

    @SerializedName("overall_assessment")
    private String overallAssessment;

    @SerializedName("control_output")
    private String controlOutput;

    public List<CryptoAnalysisIssue> getIssues() {
        return issues;
    }

    public String getOverallAssessment() {
        return overallAssessment;
    }

    public String getControlOutput() {
        return controlOutput;
    }
}

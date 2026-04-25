package ai.explainable.components.deobfuscation;

import com.google.gson.annotations.SerializedName;

public class DeobfuscationResult {

    @SerializedName("summary")
    private String summary;

    @SerializedName(
        value = "clean_code",
        alternate = {
            "deobfuscated_code",
            "simplified_code",
            "rewritten_code",
            "output_code",
        }
    )
    private String cleanCode;

    @SerializedName(
        value = "changes_summary",
        alternate = { "transformation_summary", "notes" }
    )
    private String changesSummary;

    @SerializedName("control_output")
    private String controlOutput;

    public String getSummary() {
        return summary;
    }

    public String getCleanCode() {
        return cleanCode;
    }

    public String getChangesSummary() {
        return changesSummary;
    }

    public boolean hasCleanCode() {
        return cleanCode != null && !cleanCode.trim().isEmpty();
    }

    public String getControlOutput() {
        return controlOutput;
    }
}

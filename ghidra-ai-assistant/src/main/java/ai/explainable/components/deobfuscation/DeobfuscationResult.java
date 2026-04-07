package ai.explainable.components.deobfuscation;

import com.google.gson.annotations.SerializedName;

public class DeobfuscationResult {
    @SerializedName("summary")
    private String summary;

    public String getSummary() { return summary; }
}

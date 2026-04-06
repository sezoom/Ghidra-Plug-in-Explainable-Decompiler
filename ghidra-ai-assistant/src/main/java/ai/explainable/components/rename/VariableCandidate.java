package ai.explainable.components.rename;

import com.google.gson.annotations.SerializedName;

public class VariableCandidate {
    @SerializedName("target_id")
    private final String targetId;
    @SerializedName("kind")
    private final String kind;
    @SerializedName("current_name")
    private final String currentName;
    @SerializedName("data_type")
    private final String dataType;
    @SerializedName("storage")
    private final String storage;
    @SerializedName("first_use")
    private final String firstUse;
    @SerializedName("source_type")
    private final String sourceType;
    @SerializedName("is_auto_name")
    private final boolean autoName;
    @SerializedName("token_count")
    private final int tokenCount;

    public VariableCandidate(String targetId, String kind, String currentName, String dataType,
            String storage, String firstUse, String sourceType, boolean autoName, int tokenCount) {
        this.targetId = targetId;
        this.kind = kind;
        this.currentName = currentName;
        this.dataType = dataType;
        this.storage = storage;
        this.firstUse = firstUse;
        this.sourceType = sourceType;
        this.autoName = autoName;
        this.tokenCount = tokenCount;
    }
}

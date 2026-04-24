package ai.explainable.components.rename;

import com.google.gson.annotations.SerializedName;

public class RenameItem {

    @SerializedName("target_id")
    private String targetId;

    @SerializedName("kind")
    private String kind;

    @SerializedName("old_name")
    private String oldName;

    @SerializedName("new_name")
    private String newName;

    @SerializedName("explanation")
    private String explanation;

    public String getTargetId() {
        return targetId;
    }

    public String getKind() {
        return kind;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public String getExplanation() {
        return explanation;
    }
}

package ai.explainable.components.rename;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RenameResult {

    @SerializedName("function_rename")
    private RenameItem functionRename;

    @SerializedName("variable_renames")
    private List<RenameItem> variableRenames;

    @SerializedName("summary")
    private String summary;

    @SerializedName("control_output") // ← new
    private String controlOutput;

    @SerializedName("control_attempts")
    private int controlAttempts;

    public int getControlAttempts() {
        return controlAttempts;
    }

    public RenameItem getFunctionRename() {
        return functionRename;
    }

    public List<RenameItem> getVariableRenames() {
        return variableRenames;
    }

    public String getSummary() {
        return summary;
    }

    public String getControlOutput() {
        return controlOutput;
    }
}

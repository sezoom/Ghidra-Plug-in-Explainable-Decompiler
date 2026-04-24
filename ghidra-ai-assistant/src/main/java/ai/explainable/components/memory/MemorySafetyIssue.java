package ai.explainable.components.memory;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MemorySafetyIssue {

    @SerializedName("issue_type")
    private String issueType;

    @SerializedName("description")
    private String description;

    @SerializedName("location")
    private String location;

    @SerializedName("severity")
    private String severity;

    @SerializedName("suggestion")
    private String suggestion;

    @SerializedName("functions_involved")
    private List<String> functionsInvolved;

    @SerializedName("local_variables_involved")
    private List<String> localVariablesInvolved;

    @SerializedName("calls_involved")
    private List<String> callsInvolved;

    public String getIssueType() {
        return issueType;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public List<String> getFunctionsInvolved() {
        return functionsInvolved;
    }

    public List<String> getLocalVariablesInvolved() {
        return localVariablesInvolved;
    }

    public List<String> getCallsInvolved() {
        return callsInvolved;
    }
}

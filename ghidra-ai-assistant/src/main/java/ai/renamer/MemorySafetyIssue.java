package ai.renamer;

import com.google.gson.annotations.SerializedName;

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

    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getSeverity() { return severity; }
    public String getSuggestion() { return suggestion; }
}

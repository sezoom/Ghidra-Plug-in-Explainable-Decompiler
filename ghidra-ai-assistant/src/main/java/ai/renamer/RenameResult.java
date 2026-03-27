package ai.renamer;

import java.util.Map;

public class RenameResult {
    private String newFunctionName;
    private Map<String, String> variableRenames;
    private String explanation;

    public String getNewFunctionName() { return newFunctionName; }
    public Map<String, String> getVariableRenames() { return variableRenames; }
    public String getExplanation() { return explanation; }
}
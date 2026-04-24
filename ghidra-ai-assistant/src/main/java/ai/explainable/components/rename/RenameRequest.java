package ai.explainable.components.rename;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RenameRequest {

    @SerializedName("decompiled_code")
    private final String decompiledCode;

    @SerializedName("function_name")
    private final String functionName;

    @SerializedName("variables")
    private final List<VariableCandidate> variables;

    @SerializedName("source_json_path")
    private final String sourceJsonPath;

    public RenameRequest(
        String decompiledCode,
        String functionName,
        List<VariableCandidate> variables,
        String sourceJsonPath
    ) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
        this.variables = variables;
        this.sourceJsonPath = sourceJsonPath;
    }
}

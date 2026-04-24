package ai.explainable.components.memory;

import com.google.gson.annotations.SerializedName;

public class MemorySafetyRequest {

    @SerializedName("decompiled_code")
    private final String decompiledCode;

    @SerializedName("function_name")
    private final String functionName;

    @SerializedName("source_json_path")
    private final String sourceJsonPath;

    public MemorySafetyRequest(
        String decompiledCode,
        String functionName,
        String sourceJsonPath
    ) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
        this.sourceJsonPath = sourceJsonPath;
    }
}

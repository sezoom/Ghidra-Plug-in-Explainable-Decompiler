package ai.explainable.components.deobfuscation;

import com.google.gson.annotations.SerializedName;

public class DeobfuscationRequest {

    @SerializedName("decompiled_code")
    private final String decompiledCode;

    @SerializedName("function_name")
    private final String functionName;

    @SerializedName("source_json_path")
    private final String sourceJsonPath;

    public DeobfuscationRequest(
        String decompiledCode,
        String functionName,
        String sourceJsonPath
    ) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
        this.sourceJsonPath = sourceJsonPath;
    }
}

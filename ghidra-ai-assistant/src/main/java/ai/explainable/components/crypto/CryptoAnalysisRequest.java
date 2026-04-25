package ai.explainable.components.crypto;

import com.google.gson.annotations.SerializedName;

public class CryptoAnalysisRequest {

    @SerializedName("decompiled_code")
    private final String decompiledCode;

    @SerializedName("function_name")
    private final String functionName;

    @SerializedName("source_json_path")
    private final String sourceJsonPath;

    public CryptoAnalysisRequest(
        String decompiledCode,
        String functionName,
        String sourceJsonPath
    ) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
        this.sourceJsonPath = sourceJsonPath;
    }
}

package ai.explainable.components.crypto;

import com.google.gson.annotations.SerializedName;

public class CryptoAnalysisRequest {
    @SerializedName("decompiled_code")
    private final String decompiledCode;

    @SerializedName("function_name")
    private final String functionName;

    public CryptoAnalysisRequest(String decompiledCode, String functionName) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
    }
}
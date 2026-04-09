package ai.explainable.components.deobfuscation;

import com.google.gson.annotations.SerializedName;

public class DeobfuscationRequest {
    @SerializedName("decompiled_code")
    private final String decompiledCode;
    @SerializedName("function_name")
    private final String functionName;

    public DeobfuscationRequest(String decompiledCode, String functionName) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
    }
}

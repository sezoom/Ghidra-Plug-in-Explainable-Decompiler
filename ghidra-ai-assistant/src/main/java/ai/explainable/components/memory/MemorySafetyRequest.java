package ai.explainable.components.memory;

import com.google.gson.annotations.SerializedName;

public class MemorySafetyRequest {
    @SerializedName("decompiled_code")
    private final String decompiledCode;
    @SerializedName("function_name")
    private final String functionName;

    public MemorySafetyRequest(String decompiledCode, String functionName) {
        this.decompiledCode = decompiledCode;
        this.functionName = functionName;
    }
}

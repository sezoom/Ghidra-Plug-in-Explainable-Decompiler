package ai.explainable.plugin;

import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.HighSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AnalysisContext {
    private static final Pattern AUTO_NAME_PATTERN = Pattern.compile(
        "^(param_\\d+|local_[0-9a-fA-F]+|local_\\d+|[A-Za-z]{1,4}Var\\d+|[A-Za-z]{1,4}Stack_[0-9a-fA-F]+|uStack_[0-9a-fA-F]+|auStack_[0-9a-fA-F]+|extraout_.+|unaff_.+|in_.+|DAT_[0-9a-fA-F]+|UNK_[0-9a-fA-F]+)$");

    private final Program program;
    private final Function function;
    private final String decompiledCode;
    private final DecompileResults decompileResults;
    private final HighFunction highFunction;
    private final Map<String, VariableTarget> variableTargetsById;

    public AnalysisContext(Program program, Function function, String decompiledCode,
            DecompileResults decompileResults, HighFunction highFunction,
            Map<String, VariableTarget> variableTargetsById) {
        this.program = program;
        this.function = function;
        this.decompiledCode = decompiledCode;
        this.decompileResults = decompileResults;
        this.highFunction = highFunction;
        this.variableTargetsById = Collections.unmodifiableMap(variableTargetsById);
    }

    public Program getProgram() { return program; }
    public Function getFunction() { return function; }
    public String getDecompiledCode() { return decompiledCode; }
    public DecompileResults getDecompileResults() { return decompileResults; }
    public HighFunction getHighFunction() { return highFunction; }
    public Map<String, VariableTarget> getVariableTargetsById() { return variableTargetsById; }

    public static boolean isAutoName(String name) {
        return AUTO_NAME_PATTERN.matcher(safeTrim(name)).matches();
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static class VariableTarget {
        private final String targetId;
        private final String kind;
        private final String currentName;
        private final String dataType;
        private final String storage;
        private final String firstUse;
        private final String sourceType;
        private final boolean autoName;
        private final HighSymbol highSymbol;
        private final List<ClangToken> tokens = new ArrayList<>();

        public VariableTarget(String targetId, String kind, String currentName, String dataType,
                String storage, String firstUse, String sourceType, boolean autoName,
                HighSymbol highSymbol) {
            this.targetId = targetId;
            this.kind = kind;
            this.currentName = currentName;
            this.dataType = dataType;
            this.storage = storage;
            this.firstUse = firstUse;
            this.sourceType = sourceType;
            this.autoName = autoName;
            this.highSymbol = highSymbol;
        }

        public String getTargetId() { return targetId; }
        public String getKind() { return kind; }
        public String getCurrentName() { return currentName; }
        public String getDataType() { return dataType; }
        public String getStorage() { return storage; }
        public String getFirstUse() { return firstUse; }
        public String getSourceType() { return sourceType; }
        public boolean isAutoName() { return autoName; }
        public HighSymbol getHighSymbol() { return highSymbol; }
        public List<ClangToken> getTokens() { return tokens; }
    }
}

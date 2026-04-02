package ai.renamer;

public interface AiService {
    RenameResult performRename(String decompiledCode, String currentFunctionName,
        java.util.List<VariableCandidate> variables);
    MemorySafetyResult performMemorySafetyAnalysis(String decompiledCode, String currentFunctionName);
}

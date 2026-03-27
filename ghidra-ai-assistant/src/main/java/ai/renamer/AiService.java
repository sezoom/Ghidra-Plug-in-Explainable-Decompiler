package ai.renamer;

public interface AiService {
    RenameResult performRename(String decompiledCode, String currentFunctionName);
    MemorySafetyResult performMemorySafetyAnalysis(String decompiledCode, String currentFunctionName);
}
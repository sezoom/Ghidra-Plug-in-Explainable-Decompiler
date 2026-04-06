package ai.explainable.decompiler;

import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;

public class DecompileHelper {
    private DecompileHelper() {}

    public static DecompileResults decompile(Function function, Program program, TaskMonitor monitor) {
        DecompInterface decomp = new DecompInterface();
        decomp.setOptions(new DecompileOptions());
        decomp.openProgram(program);
        return decomp.decompileFunction(function, 30, monitor);
    }
}

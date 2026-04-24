package ai.explainable.decompiler;

import com.google.gson.*;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.framework.Application;
import ghidra.program.model.address.Address;
import ghidra.program.model.block.BasicBlockModel;
import ghidra.program.model.block.CodeBlock;
import ghidra.program.model.block.CodeBlockIterator;
import ghidra.program.model.block.CodeBlockReference;
import ghidra.program.model.block.CodeBlockReferenceIterator;
import ghidra.program.model.data.Pointer;
import ghidra.program.model.data.StringDataType;
import ghidra.program.model.data.StringDataType;
import ghidra.program.model.listing.*;
import ghidra.program.model.scalar.Scalar;
import ghidra.program.model.symbol.*;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DecompileHelper {

    private DecompileHelper() {}

    // ── existing method ────────────────────────────────────────────────────────
    public static DecompileResults decompile(
        Function function,
        Program program,
        TaskMonitor monitor
    ) {
        DecompInterface decomp = new DecompInterface();
        decomp.setOptions(new DecompileOptions());
        decomp.openProgram(program);
        return decomp.decompileFunction(function, 30, monitor);
    }

    // -------------------------------------------------------------------------
    // BUILD
    // -------------------------------------------------------------------------

    /**
     * Build a JSON snapshot for a single decompiled function.
     *
     * @param program  the currently open Ghidra Program
     * @param func     the Function to snapshot
     * @param monitor  task monitor (used by the decompiler)
     * @return         a JsonObject containing all extracted metadata
     */
    public static JsonObject buildSnapshot(
        Program program,
        Function func,
        TaskMonitor monitor
    ) {
        JsonObject root = new JsonObject();

        // ── Basic identity ────────────────────────────────────────────────────
        root.addProperty("function_name", func.getName());
        root.addProperty("address", func.getEntryPoint().toString());
        root.addProperty(
            "signature",
            func.getSignature().getPrototypeString(false, false)
        );
        root.addProperty("return_type", func.getReturnType().getName());

        // ── Parameters ────────────────────────────────────────────────────────
        JsonArray params = new JsonArray();
        for (Variable p : func.getParameters()) {
            JsonObject param = new JsonObject();
            param.addProperty("name", p.getName());
            param.addProperty("type", p.getDataType().getName());
            param.addProperty("size", p.getLength());
            param.addProperty("ordinal", p.getOrdinal());
            params.add(param);
        }
        root.add("parameters", params);

        // ── Local variables ───────────────────────────────────────────────────
        JsonArray locals = new JsonArray();
        for (Variable v : func.getAllVariables()) {
            // Skip parameters — they are already captured above
            if (v.isParameter()) continue;

            JsonObject local = new JsonObject();
            local.addProperty("name", v.getName());
            local.addProperty("type", v.getDataType().getName());
            local.addProperty("size", v.getLength());

            // Stack variables have a meaningful offset; registers do not
            if (v.isStackVariable()) {
                local.addProperty(
                    "storage",
                    "Stack[" + v.getStackOffset() + "]"
                );
            } else {
                local.addProperty("storage", v.getVariableStorage().toString());
            }
            locals.add(local);
        }
        root.add("local_variables", locals);

        // ── Outgoing calls (CALL cross-references) ────────────────────────────
        JsonArray calls = new JsonArray();
        ReferenceManager refMgr = program.getReferenceManager();
        Listing listing = program.getListing();

        // Iterate every instruction address inside this function body
        for (Address instrAddr : func.getBody().getAddresses(true)) {
            for (Reference ref : refMgr.getReferencesFrom(instrAddr)) {
                if (!ref.getReferenceType().isCall()) continue;

                Address dest = ref.getToAddress();
                Function callee = listing.getFunctionAt(dest);

                JsonObject call = new JsonObject();
                call.addProperty("address", instrAddr.toString());
                call.addProperty(
                    "callee",
                    callee != null ? callee.getName() : dest.toString()
                );
                calls.add(call);
            }
        }
        root.add("calls", calls);

        // ── Decompiled source code ────────────────────────────────────────────
        String decompiledCode = decompile(program, func, monitor);
        root.addProperty("decompiled_code", decompiledCode);

        return root;
    }

    // -------------------------------------------------------------------------
    // SAVE
    // -------------------------------------------------------------------------

    /**
     * Serialise a snapshot JsonObject to a pretty-printed .json file.
     * The file is named after the function (e.g. "main.json") and written
     * into {@code outputDir}.
     *
     * @param snapshot   the JsonObject produced by {@link #buildSnapshot}
     * @param outputDir  absolute or relative path to the destination directory
     */
    public static void saveSnapshot(JsonObject snapshot, String outputDir) {
        // Derive a safe filename from the function name stored in the snapshot
        String functionName = snapshot.has("function_name")
            ? snapshot.get("function_name").getAsString()
            : "unknown_function";

        // Replace characters that are invalid in filenames
        String safeFileName =
            functionName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".json";

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs(); // create the directory tree if it doesn't exist
        }

        File outputFile = new File(dir, safeFileName);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(snapshot, writer);
            System.out.println(
                "[FunctionSnapshot] Saved → " + outputFile.getAbsolutePath()
            );
        } catch (IOException e) {
            System.err.println(
                "[FunctionSnapshot] ERROR writing file: " + e.getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    /**
     * Run Ghidra's decompiler on {@code func} and return the C source string.
     * Returns an empty string on failure rather than throwing.
     */
    private static String decompile(
        Program program,
        Function func,
        TaskMonitor monitor
    ) {
        DecompInterface decomp = new DecompInterface();
        try {
            DecompileOptions options = new DecompileOptions();
            decomp.setOptions(options);

            if (!decomp.openProgram(program)) {
                System.err.println(
                    "[FunctionSnapshot] Decompiler failed to open program."
                );
                return "";
            }

            DecompileResults results = decomp.decompileFunction(
                func,
                60,
                monitor
            );
            if (results == null || !results.decompileCompleted()) {
                System.err.println(
                    "[FunctionSnapshot] Decompile did not complete for: " +
                        func.getName()
                );
                return "";
            }

            return results.getDecompiledFunction().getC();
        } finally {
            decomp.dispose(); // always release native decompiler resources
        }
    }

    // -------------------------------------------------------------------------
    // EXAMPLE ENTRY POINT (for a Ghidra Script context)
    // -------------------------------------------------------------------------
    // If you are calling this from a GhidraScript subclass, add the following
    // to your run() method:
    //
    //   Function func = currentProgram.getListing()
    //                                 .getFunctionContaining(currentAddress);
    //   if (func != null) {
    //       JsonObject snap = FunctionSnapshot.buildSnapshot(
    //                             currentProgram, func, monitor);
    //       FunctionSnapshot.saveSnapshot(snap, "/tmp/ghidra_snapshots");
    //   }
}

package ai.explainable.decompiler;

import com.google.gson.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.DecompiledFunction;
import ghidra.framework.Application;
import ghidra.framework.Application;
import ghidra.program.model.address.Address;
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
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.listing.Program;
import ghidra.program.model.scalar.Scalar;
import ghidra.program.model.symbol.*;
import ghidra.program.model.symbol.RefType;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceManager;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolType;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import ghidra.util.task.TaskMonitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static JsonObject buildSnapshot(
        Program program,
        Function func,
        TaskMonitor monitor
    ) throws Exception {
        JsonObject root = new JsonObject();

        // meta
        JsonObject meta = new JsonObject();
        meta.addProperty("binary", program.getName());
        meta.addProperty(
            "arch",
            program.getLanguage().getProcessor().toString() +
                "_" +
                program.getLanguage().getLanguageDescription().getSize()
        );
        meta.addProperty("base_addr", formatAddress(program.getImageBase()));
        meta.addProperty("hash", "sha256:" + program.getExecutableSHA256());
        meta.addProperty("ghidra_version", Application.getApplicationVersion());
        root.add("meta", meta);

        // decompile first; use this as the source of truth
        String cText = decompileToC(program, func, monitor);

        JsonObject f = new JsonObject();
        f.addProperty("name", func.getName());
        f.addProperty("addr", formatAddress(func.getEntryPoint()));
        f.addProperty("exists", true);

        // signature: keep only what is visible/explicit
        JsonObject sig = new JsonObject();
        sig.addProperty("ret", func.getReturnType().getName());

        JsonArray params = new JsonArray();
        for (var p : func.getParameters()) {
            JsonObject po = new JsonObject();
            po.addProperty("type", p.getDataType().getName());
            po.addProperty("name", p.getName());
            params.add(po);
        }
        sig.add("params", params);
        f.add("signature", sig);

        // locals: parse declarations from the decompiled C text
        f.add("locals", extractDecompilerLocals(cText));

        // calls: parse visible function calls from the decompiled C text
        f.add("calls", extractVisibleCalls(program, cText));

        // strings_used: only string literals visible in the decompiled C text
        f.add("strings_used", extractStringLiterals(cText));

        // constants_used: only numeric constants visible in the decompiled C text
        f.add("constants_used", extractVisibleConstants(cText));

        // globals_used: only direct non-function, non-string-symbol references
        f.add("globals_used", extractEssentialGlobals(program, func));

        // returns: only explicit return statements from the decompiled C text
        f.add("returns", extractVisibleReturns(cText));

        // optional: keep the decompiled C itself if you want exact traceability
        f.addProperty("decompiled_c", cText);

        JsonArray funcsArr = new JsonArray();
        funcsArr.add(f);
        root.add("functions", funcsArr);

        // symbols: only the function itself + visible callees that resolve to functions
        JsonArray symbols = new JsonArray();
        addSymbol(symbols, func.getName(), "function", func.getEntryPoint());

        Set<String> seenFuncAddrs = new HashSet<>();
        seenFuncAddrs.add(func.getEntryPoint().toString());

        JsonArray calls = f.getAsJsonArray("calls");
        for (int i = 0; i < calls.size(); i++) {
            JsonObject c = calls.get(i).getAsJsonObject();
            if (!c.has("addr")) {
                continue;
            }
            String addrStr = c.get("addr").getAsString();
            if (!seenFuncAddrs.add(addrStr)) {
                continue;
            }
            addSymbol(
                symbols,
                c.get("name").getAsString(),
                "function",
                addrStr
            );
        }

        root.add("symbols", symbols);
        return root;
    }

    private static String decompileToC(
        Program program,
        Function func,
        TaskMonitor monitor
    ) throws Exception {
        DecompInterface ifc = new DecompInterface();
        DecompileOptions opts = new DecompileOptions();
        ifc.setOptions(opts);

        if (!ifc.openProgram(program)) {
            throw new IllegalStateException(
                "Failed to open program in decompiler"
            );
        }

        DecompileResults res = ifc.decompileFunction(func, 60, monitor);
        if (!res.decompileCompleted()) {
            throw new IllegalStateException(
                "Decompilation failed for " +
                    func.getName() +
                    ": " +
                    res.getErrorMessage()
            );
        }

        DecompiledFunction df = res.getDecompiledFunction();
        if (df == null || df.getC() == null) {
            throw new IllegalStateException("Decompiler returned no C text");
        }

        return df.getC();
    }

    private static JsonArray extractDecompilerLocals(String cText) {
        JsonArray arr = new JsonArray();

        // Look only inside the outermost function body
        String body = extractFunctionBody(cText);
        if (body == null) {
            return arr;
        }

        // Local declarations in decompiler output usually appear as:
        //   char acStack_78 [64];
        //   long local_18;
        // We intentionally keep this minimal and do not try to infer more.
        Pattern p = Pattern.compile(
            "(?m)^\\s*([A-Za-z_][A-Za-z0-9_\\s\\*]*?)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(\\[[^;\\]]+\\])?\\s*;"
        );

        Matcher m = p.matcher(body);
        while (m.find()) {
            String type = cleanSpace(m.group(1));
            String name = m.group(2);
            String suffix = m.group(3);

            // Skip obvious statements that are not declarations
            if (isKeyword(type)) {
                continue;
            }

            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            o.addProperty(
                "type",
                suffix == null ? type : type + " " + suffix.trim()
            );
            arr.add(o);
        }

        return arr;
    }

    private static JsonArray extractVisibleCalls(
        Program program,
        String cText
    ) {
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();

        Pattern p = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
        Matcher m = p.matcher(cText);

        Set<String> banned = Set.of(
            "if",
            "for",
            "while",
            "switch",
            "return",
            "sizeof"
        );

        while (m.find()) {
            String name = m.group(1);
            if (banned.contains(name)) {
                continue;
            }
            if (!seen.add(name)) {
                continue;
            }

            JsonObject c = new JsonObject();
            c.addProperty("name", name);

            Function callee = findFunctionByName(program, name);
            if (callee != null) {
                c.addProperty("addr", formatAddress(callee.getEntryPoint()));
            }

            arr.add(c);
        }

        return arr;
    }

    private static Function findFunctionByName(Program program, String name) {
        FunctionIterator fit = program.getFunctionManager().getFunctions(true);
        while (fit.hasNext()) {
            Function f = fit.next();
            if (name.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }

    private static JsonArray extractStringLiterals(String cText) {
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();

        Pattern p = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
        Matcher m = p.matcher(cText);

        while (m.find()) {
            String raw = m.group();
            String unquoted = raw.substring(1, raw.length() - 1);
            if (seen.add(unquoted)) {
                arr.add(unquoted);
            }
        }

        return arr;
    }

    private static JsonArray extractVisibleConstants(String cText) {
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();

        // Hex and decimal integer literals visible in decompiled C.
        Pattern p = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+)\\b");
        Matcher m = p.matcher(cText);

        while (m.find()) {
            String lit = m.group(1);

            // Keep only meaningful visible constants
            if (lit.equals("0") || lit.equals("1")) {
                continue;
            }

            if (!seen.add(lit)) {
                continue;
            }

            JsonObject co = new JsonObject();
            if (lit.startsWith("0x") || lit.startsWith("0X")) {
                long value = Long.parseUnsignedLong(lit.substring(2), 16);
                co.addProperty("hex", "0x" + Long.toHexString(value));
                co.addProperty("decimal", value);
            } else {
                long value = Long.parseLong(lit);
                co.addProperty("hex", "0x" + Long.toHexString(value));
                co.addProperty("decimal", value);
            }
            arr.add(co);
        }

        return arr;
    }

    private static JsonArray extractEssentialGlobals(
        Program program,
        Function func
    ) {
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();

        ReferenceManager rm = program.getReferenceManager();
        InstructionIterator iit = program
            .getListing()
            .getInstructions(func.getBody(), true);

        while (iit.hasNext()) {
            Instruction instr = iit.next();

            Reference[] refs = rm.getReferencesFrom(instr.getAddress());
            for (Reference ref : refs) {
                if (ref == null || !ref.isMemoryReference()) {
                    continue;
                }

                Address to = ref.getToAddress();
                if (to == null) {
                    continue;
                }

                Symbol sym = program.getSymbolTable().getPrimarySymbol(to);
                if (sym == null) {
                    continue;
                }

                if (sym.getSymbolType() == SymbolType.FUNCTION) {
                    continue;
                }

                String name = sym.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                if (name.startsWith("s_")) {
                    continue;
                }

                if (seen.add(name)) {
                    arr.add(name);
                }
            }
        }

        return arr;
    }

    private static JsonArray extractVisibleReturns(String cText) {
        JsonArray arr = new JsonArray();
        Set<String> seen = new LinkedHashSet<>();

        Pattern p = Pattern.compile("\\breturn\\s+([^;]+);");
        Matcher m = p.matcher(cText);

        while (m.find()) {
            String value = cleanSpace(m.group(1));
            if (!seen.add(value)) {
                continue;
            }

            JsonObject ret = new JsonObject();
            ret.addProperty("value", value);
            ret.addProperty("conditional", false);
            arr.add(ret);
        }

        return arr;
    }

    private static String extractFunctionBody(String cText) {
        int firstBrace = cText.indexOf('{');
        int lastBrace = cText.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            return null;
        }
        return cText.substring(firstBrace + 1, lastBrace);
    }

    private static boolean isKeyword(String s) {
        String x = s.trim();
        return (
            x.equals("if") ||
            x.equals("for") ||
            x.equals("while") ||
            x.equals("switch") ||
            x.equals("return")
        );
    }

    private static String cleanSpace(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static void addSymbol(
        JsonArray arr,
        String name,
        String kind,
        Address addr
    ) {
        JsonObject s = new JsonObject();
        s.addProperty("name", name);
        s.addProperty("kind", kind);
        s.addProperty("addr", formatAddress(addr));
        arr.add(s);
    }

    private static void addSymbol(
        JsonArray arr,
        String name,
        String kind,
        String addr
    ) {
        JsonObject s = new JsonObject();
        s.addProperty("name", name);
        s.addProperty("kind", kind);
        s.addProperty("addr", addr.startsWith("0x") ? addr : "0x" + addr);
        arr.add(s);
    }

    private static String formatAddress(Address a) {
        return "0x" + a.toString();
    }

    public static Path saveSnapshot(JsonObject snapshot, String outputDir)
        throws Exception {
        String binary = snapshot
            .getAsJsonObject("meta")
            .get("binary")
            .getAsString();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path out = Paths.get(outputDir, binary + "_" + ts + ".json");
        Files.createDirectories(out.getParent());
        Files.writeString(
            out,
            new GsonBuilder().setPrettyPrinting().create().toJson(snapshot)
        );
        return out;
    }
}

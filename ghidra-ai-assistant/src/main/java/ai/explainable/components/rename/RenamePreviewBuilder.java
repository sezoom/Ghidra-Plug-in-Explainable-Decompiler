package ai.explainable.components.rename;

import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;
import ghidra.app.decompiler.ClangFuncNameToken;
import ghidra.app.decompiler.ClangLine;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.ClangTokenGroup;
import ghidra.app.decompiler.ClangVariableToken;
import ghidra.app.decompiler.component.DecompilerUtils;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.HighSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RenamePreviewBuilder {
    private RenamePreviewBuilder() {}

    public static PreviewData build(AnalysisContext context, RenameResult result) {
        if (result == null || context.getDecompileResults() == null || context.getHighFunction() == null) {
            return new PreviewData(context.getDecompiledCode(), Collections.emptyList());
        }

        ClangTokenGroup markup = context.getDecompileResults().getCCodeMarkup();
        if (markup == null) {
            return new PreviewData(context.getDecompiledCode(), Collections.emptyList());
        }

        RenameItem functionRename = getApplicableFunctionRename(context, result);
        Map<String, RenameItem> variableRenames = new LinkedHashMap<>();
        for (RenameItem item : getApplicableVariableRenames(context, result)) {
            variableRenames.put(item.getTargetId(), item);
        }

        StringBuilder preview = new StringBuilder();
        List<AnalysisView.HighlightSpan> spans = new ArrayList<>();
        List<ClangLine> lines = DecompilerUtils.toLines(markup);
        for (ClangLine line : lines) {
            preview.append(line.getIndentString());
            for (ClangToken token : line.getAllTokens()) {
                RenameItem item = getRenameForToken(context, token, functionRename, variableRenames);
                String tokenText = item != null ? item.getNewName() : token.getText();
                int start = preview.length();
                preview.append(tokenText);
                if (item != null) {
                    spans.add(new AnalysisView.HighlightSpan(start, preview.length(), buildTooltip(item)));
                }
            }
            preview.append(System.lineSeparator());
        }
        return new PreviewData(preview.toString(), spans);
    }

    public static RenameItem getApplicableFunctionRename(AnalysisContext context, RenameResult result) {
        if (result == null || context.getFunction() == null) {
            return null;
        }
        RenameItem item = result.getFunctionRename();
        if (!isValid(item)) {
            return null;
        }
        String currentName = context.getFunction().getName();
        if (!safeTrim(item.getOldName()).equals(currentName)) {
            return null;
        }
        if (safeTrim(item.getNewName()).equals(currentName)) {
            return null;
        }
        return item;
    }

    public static List<RenameItem> getApplicableVariableRenames(AnalysisContext context, RenameResult result) {
        if (result == null || result.getVariableRenames() == null) {
            return Collections.emptyList();
        }
        List<RenameItem> applicable = new ArrayList<>();
        for (RenameItem item : result.getVariableRenames()) {
            if (!isValid(item)) {
                continue;
            }
            AnalysisContext.VariableTarget target = context.getVariableTargetsById().get(safeTrim(item.getTargetId()));
            if (target == null) {
                continue;
            }
            if (!safeTrim(item.getOldName()).equals(target.getCurrentName())) {
                continue;
            }
            if (!safeTrim(item.getKind()).isEmpty() && !safeTrim(item.getKind()).equals(target.getKind())) {
                continue;
            }
            if (safeTrim(item.getNewName()).equals(target.getCurrentName())) {
                continue;
            }
            applicable.add(item);
        }
        return applicable;
    }

    private static RenameItem getRenameForToken(AnalysisContext context, ClangToken token,
            RenameItem functionRename, Map<String, RenameItem> variableRenamesByTargetId) {
        if (functionRename != null && token instanceof ClangFuncNameToken funcToken && isCurrentFunctionToken(context, funcToken)) {
            return functionRename;
        }
        if (!(token instanceof ClangVariableToken) || context.getHighFunction() == null) {
            return null;
        }
        HighFunction highFunction = context.getHighFunction();
        HighSymbol highSymbol = token.getHighSymbol(highFunction);
        if (highSymbol == null || highSymbol.isThisPointer() || highSymbol.isHiddenReturn()) {
            return null;
        }
        return variableRenamesByTargetId.get(buildTargetId(highSymbol));
    }

    private static boolean isCurrentFunctionToken(AnalysisContext context, ClangFuncNameToken token) {
        if (context.getFunction() == null) {
            return false;
        }
        HighFunction tokenFunction = token.getHighFunction();
        if (tokenFunction != null && tokenFunction.getFunction() != null) {
            return context.getFunction().equals(tokenFunction.getFunction());
        }
        return safeTrim(token.getText()).equals(context.getFunction().getName());
    }

    public static String buildTooltip(RenameItem item) {
        String kind = safeTrim(item.getKind());
        String label = kind.isEmpty() ? safeTrim(item.getOldName()) + " -> " + safeTrim(item.getNewName())
            : kind + ": " + safeTrim(item.getOldName()) + " -> " + safeTrim(item.getNewName());
        return "<html><b>" + escapeHtml(label) + "</b><br/>" + escapeHtml(safeTrim(item.getExplanation())) + "</html>";
    }

    private static boolean isValid(RenameItem item) {
        return item != null && !safeTrim(item.getOldName()).isEmpty() && !safeTrim(item.getNewName()).isEmpty();
    }

    private static String buildTargetId(HighSymbol highSymbol) {
        if (highSymbol.isParameter()) {
            return "parameter:" + highSymbol.getCategoryIndex();
        }
        if (highSymbol.isGlobal()) {
            return "global:" + highSymbol.getStorage().getFirstVarnode().getAddress();
        }
        String pcAddress = highSymbol.getPCAddress() == null ? "entry" : highSymbol.getPCAddress().toString();
        return "local:" + highSymbol.getStorage() + "@" + pcAddress;
    }

    private static String safeTrim(String value) {
        return AnalysisContext.safeTrim(value);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>");
    }

    public record PreviewData(String text, List<AnalysisView.HighlightSpan> spans) {}
}

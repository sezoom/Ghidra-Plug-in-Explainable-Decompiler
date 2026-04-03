package ai.renamer;

import docking.widgets.OptionDialog;
import ghidra.app.decompiler.ClangFuncNameToken;
import ghidra.app.decompiler.ClangLine;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.ClangTokenGroup;
import ghidra.app.decompiler.ClangVariableToken;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.component.DecompilerUtils;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.Variable;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.HighFunctionDBUtil;
import ghidra.program.model.pcode.HighSymbol;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;
import ghidra.util.task.TaskLauncher;
import ghidra.util.task.TaskMonitor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AIRenamerProvider extends ComponentProviderAdapter {
    private static final Color HIGHLIGHT_BG = new Color(255, 243, 176);
    private static final Color HIGHLIGHT_FG = new Color(102, 60, 0);
    private static final Pattern AUTO_NAME_PATTERN = Pattern.compile(
        "^(param_\\d+|local_[0-9a-fA-F]+|local_\\d+|[A-Za-z]{1,4}Var\\d+|[A-Za-z]{1,4}Stack_[0-9a-fA-F]+|uStack_[0-9a-fA-F]+|auStack_[0-9a-fA-F]+|extraout_.+|unaff_.+|in_.+|DAT_[0-9a-fA-F]+|UNK_[0-9a-fA-F]+)$");

    private final AIRenamerPlugin plugin;
    private final AiService aiService = new HttpAiService("http://127.0.0.1:8000");

    private JPanel mainPanel;
    private PreviewTextPane codeArea;
    private JTextArea resultArea;
    private JButton loadBtn;
    private JButton renameBtn;
    private JButton applyRenameBtn;
    private JButton safetyBtn;

    private Program currentProgram;
    private Function currentFunction;
    private String loadedCode = "";
    private DecompileResults lastDecompileResults;
    private HighFunction lastHighFunction;
    private Map<String, VariableTarget> variableTargetsById = Collections.emptyMap();

    private RenameResult lastRenameResult;
    private MemorySafetyResult lastSafetyResult;
    private List<RenameSpan> renameSpans = Collections.emptyList();

    public AIRenamerProvider(AIRenamerPlugin plugin, PluginTool tool) {
        super(tool, "AI Explainable-Decompiler", plugin.getName(), Program.class);
        this.plugin = plugin;
        setTitle("AI Explainable-Decompiler");
        buildUI();
    }

    private void buildUI() {
        mainPanel = new JPanel(new BorderLayout(0, 8));

        codeArea = new PreviewTextPane();
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setEditable(false);
        codeArea.setToolTipText(" ");
        DefaultCaret caret = (DefaultCaret) codeArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        resultArea = new JTextArea(12, 60);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(codeArea), new JScrollPane(resultArea));
        splitPane.setResizeWeight(0.70);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadBtn = new JButton("Load Current Decompilation");
        renameBtn = new JButton("AI Rename");
        applyRenameBtn = new JButton("Apply Rename Suggestions");
        safetyBtn = new JButton("Memory Safety Analysis");
        applyRenameBtn.setEnabled(false);

        toolbar.add(loadBtn);
        toolbar.add(renameBtn);
        toolbar.add(applyRenameBtn);
        toolbar.add(safetyBtn);
        mainPanel.add(toolbar, BorderLayout.NORTH);

        loadBtn.addActionListener(e -> loadCurrent());
        renameBtn.addActionListener(e -> new TaskLauncher(new AiAnalysisTask("rename")));
        applyRenameBtn.addActionListener(e -> applyRenameSuggestions());
        safetyBtn.addActionListener(e -> new TaskLauncher(new AiAnalysisTask("memory_safety")));
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    private void loadCurrent() {
        currentProgram = plugin.getCurrentProgram();
        ProgramLocation loc = plugin.getProgramLocation();
        clearAnalysisState();
        clearDecompileSnapshot();

        if (currentProgram == null || loc == null) {
            setCodePreview("No active program location.", Collections.emptyList());
            resultArea.setText("Load a function from the current cursor location to begin.");
            return;
        }

        currentFunction = currentProgram.getFunctionManager().getFunctionContaining(loc.getAddress());
        if (currentFunction == null) {
            setCodePreview("No function at current location.", Collections.emptyList());
            resultArea.setText("Move the cursor into a function and load the current decompilation.");
            return;
        }

        DecompileResults res = DecompileHelper.decompile(currentFunction, currentProgram, TaskMonitor.DUMMY);
        if (res.decompileCompleted() && res.getDecompiledFunction() != null) {
            loadedCode = res.getDecompiledFunction().getC();
            lastDecompileResults = res;
            lastHighFunction = res.getHighFunction();
            variableTargetsById = buildVariableTargets(res);
            setCodePreview(loadedCode, Collections.emptyList());
            resultArea.setText(buildLoadMessage());
            return;
        }

        loadedCode = "";
        setCodePreview("Decompile failed: " + res.getErrorMessage(), Collections.emptyList());
        resultArea.setText("Decompiler did not return C for the current function.");
    }

    private String buildLoadMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded `")
          .append(currentFunction.getName())
          .append("`.\n\nTracked variables for AI rename: ")
          .append(variableTargetsById.size());
        return sb.toString();
    }

    private void clearAnalysisState() {
        lastRenameResult = null;
        lastSafetyResult = null;
        renameSpans = Collections.emptyList();
        applyRenameBtn.setEnabled(false);
    }

    private void clearDecompileSnapshot() {
        loadedCode = "";
        currentFunction = null;
        lastDecompileResults = null;
        lastHighFunction = null;
        variableTargetsById = Collections.emptyMap();
    }

    private void applyRenameSuggestions() {
        RenameItem functionRename = getApplicableFunctionRename();
        List<RenameItem> variableRenames = getApplicableVariableRenames();
        if (currentProgram == null || currentFunction == null ||
                (functionRename == null && variableRenames.isEmpty())) {
            Msg.showInfo(this, mainPanel, "AI Rename",
                "Load a rename suggestion before applying it.");
            return;
        }

        int choice = OptionDialog.showYesNoDialog(mainPanel, "Apply AI Renames?",
            buildApplyConfirmationMessage(functionRename, variableRenames));
        if (choice != OptionDialog.OPTION_ONE) {
            return;
        }

        int appliedCount = 0;
        List<String> failures = new ArrayList<>();
        int tx = currentProgram.startTransaction("AI Rename Suggestions");
        boolean commit = false;
        try {
            if (functionRename != null) {
                currentFunction.setName(functionRename.getNewName(), SourceType.USER_DEFINED);
                appliedCount++;
            }

            for (RenameItem item : variableRenames) {
                VariableTarget target = variableTargetsById.get(item.getTargetId());
                if (target == null) {
                    failures.add(item.getOldName() + " -> " + item.getNewName() + " (missing target)");
                    continue;
                }

                try {
                    HighFunctionDBUtil.updateDBVariable(target.highSymbol, item.getNewName(), null,
                        SourceType.USER_DEFINED);
                    appliedCount++;
                }
                catch (DuplicateNameException | InvalidInputException | UnsupportedOperationException ex) {
                    failures.add(item.getOldName() + " -> " + item.getNewName() + " (" +
                        ex.getMessage() + ")");
                }
            }

            commit = appliedCount > 0;
        }
        catch (Exception ex) {
            Msg.showError(this, mainPanel, "AI Rename",
                "Failed to apply AI rename suggestions.", ex);
            return;
        }
        finally {
            currentProgram.endTransaction(tx, commit);
        }

        loadCurrent();
        resultArea.setText(buildApplySummary(appliedCount, functionRename, variableRenames, failures));
        if (appliedCount > 0) {
            Msg.showInfo(this, mainPanel, "AI Rename",
                "Applied " + appliedCount + " rename suggestion(s). Reloaded the current decompilation.");
        }
    }

    private String buildApplyConfirmationMessage(RenameItem functionRename,
            List<RenameItem> variableRenames) {
        StringBuilder sb = new StringBuilder("Apply the following AI rename suggestions?\n\n");
        if (functionRename != null) {
            sb.append("Function: ")
              .append(functionRename.getOldName())
              .append(" -> ")
              .append(functionRename.getNewName())
              .append("\n");
        }
        if (!variableRenames.isEmpty()) {
            sb.append("Variables: ").append(variableRenames.size()).append(" change(s)");
        }
        return sb.toString();
    }

    private String buildApplySummary(int appliedCount, RenameItem functionRename,
            List<RenameItem> variableRenames, List<String> failures) {
        StringBuilder sb = new StringBuilder();
        sb.append("Applied rename suggestions: ").append(appliedCount).append("\n");
        if (functionRename != null) {
            sb.append("Function: ")
              .append(functionRename.getOldName())
              .append(" -> ")
              .append(functionRename.getNewName())
              .append("\n");
        }
        if (!variableRenames.isEmpty()) {
            sb.append("Variables requested: ").append(variableRenames.size()).append("\n");
        }
        if (!failures.isEmpty()) {
            sb.append("\nSkipped:\n");
            for (String failure : failures) {
                sb.append("- ").append(failure).append("\n");
            }
        }
        return sb.toString();
    }

    private RenameItem getApplicableFunctionRename() {
        RenameItem item = getSuggestedFunctionRename();
        if (item == null || currentFunction == null) {
            return null;
        }
        if (!safeTrim(item.getOldName()).equals(currentFunction.getName())) {
            return null;
        }
        if (safeTrim(item.getNewName()).equals(currentFunction.getName())) {
            return null;
        }
        return item;
    }

    private RenameItem getSuggestedFunctionRename() {
        if (lastRenameResult == null) {
            return null;
        }
        RenameItem item = lastRenameResult.getFunctionRename();
        if (!isValidRename(item)) {
            return null;
        }
        return item;
    }

    private List<RenameItem> getApplicableVariableRenames() {
        if (lastRenameResult == null || lastRenameResult.getVariableRenames() == null) {
            return Collections.emptyList();
        }

        List<RenameItem> applicable = new ArrayList<>();
        for (RenameItem item : lastRenameResult.getVariableRenames()) {
            if (isApplicableVariableRename(item)) {
                applicable.add(item);
            }
        }
        return applicable;
    }

    private boolean isApplicableVariableRename(RenameItem item) {
        if (!isValidRename(item)) {
            return false;
        }
        String targetId = safeTrim(item.getTargetId());
        if (targetId.isEmpty()) {
            return false;
        }

        VariableTarget target = variableTargetsById.get(targetId);
        if (target == null) {
            return false;
        }
        if (!safeTrim(item.getOldName()).equals(target.currentName)) {
            return false;
        }
        if (!safeTrim(item.getKind()).isEmpty() && !safeTrim(item.getKind()).equals(target.kind)) {
            return false;
        }
        return !safeTrim(item.getNewName()).equals(target.currentName);
    }

    private boolean hasApplicableRenames() {
        return getApplicableFunctionRename() != null || !getApplicableVariableRenames().isEmpty();
    }

    private void setCodePreview(String text, List<RenameSpan> spans) {
        renameSpans = spans;
        StyledDocument document = codeArea.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setFontFamily(normal, Font.MONOSPACED);
        StyleConstants.setFontSize(normal, 12);

        SimpleAttributeSet highlight = new SimpleAttributeSet(normal);
        StyleConstants.setBackground(highlight, HIGHLIGHT_BG);
        StyleConstants.setForeground(highlight, HIGHLIGHT_FG);
        StyleConstants.setBold(highlight, true);

        try {
            document.remove(0, document.getLength());
            document.insertString(0, text, normal);
            for (RenameSpan span : spans) {
                document.setCharacterAttributes(span.start, span.end - span.start, highlight, false);
            }
            codeArea.setCaretPosition(0);
        }
        catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to update preview text", ex);
        }
    }

    private PreviewData buildRenamePreview(RenameResult renameResult) {
        if (renameResult == null || lastDecompileResults == null || lastHighFunction == null) {
            return new PreviewData(loadedCode, Collections.emptyList());
        }

        ClangTokenGroup cCodeMarkup = lastDecompileResults.getCCodeMarkup();
        if (cCodeMarkup == null) {
            return new PreviewData(loadedCode, Collections.emptyList());
        }

        RenameItem functionRename = getSuggestedFunctionRename();
        Map<String, RenameItem> variableRenamesByTargetId = new LinkedHashMap<>();
        for (RenameItem item : getApplicableVariableRenames()) {
            variableRenamesByTargetId.put(item.getTargetId(), item);
        }

        List<ClangLine> lines = DecompilerUtils.toLines(cCodeMarkup);
        StringBuilder preview = new StringBuilder();
        List<RenameSpan> spans = new ArrayList<>();

        for (ClangLine line : lines) {
            preview.append(line.getIndentString());
            for (ClangToken token : line.getAllTokens()) {
                RenameItem renameItem = getRenameForToken(token, functionRename, variableRenamesByTargetId);
                String tokenText = renameItem != null ? renameItem.getNewName() : token.getText();
                int spanStart = preview.length();
                preview.append(tokenText);
                if (renameItem != null) {
                    spans.add(new RenameSpan(spanStart, preview.length(), renameItem));
                }
            }
            preview.append(System.lineSeparator());
        }

        return new PreviewData(preview.toString(), spans);
    }

    private RenameItem getRenameForToken(ClangToken token, RenameItem functionRename,
            Map<String, RenameItem> variableRenamesByTargetId) {
        if (functionRename != null && token instanceof ClangFuncNameToken &&
                isCurrentFunctionToken((ClangFuncNameToken) token)) {
            return functionRename;
        }

        if (!(token instanceof ClangVariableToken) || lastHighFunction == null) {
            return null;
        }

        HighSymbol highSymbol = token.getHighSymbol(lastHighFunction);
        if (!isTrackableVariable(highSymbol)) {
            return null;
        }

        return variableRenamesByTargetId.get(buildTargetId(highSymbol));
    }

    private boolean isCurrentFunctionToken(ClangFuncNameToken token) {
        if (currentFunction == null) {
            return false;
        }
        HighFunction tokenFunction = token.getHighFunction();
        if (tokenFunction != null && tokenFunction.getFunction() != null) {
            return currentFunction.equals(tokenFunction.getFunction());
        }
        return safeTrim(token.getText()).equals(currentFunction.getName());
    }

    private Map<String, VariableTarget> buildVariableTargets(DecompileResults decompileResults) {
        if (decompileResults == null || decompileResults.getCCodeMarkup() == null ||
                decompileResults.getHighFunction() == null) {
            return Collections.emptyMap();
        }

        Map<String, VariableTarget> targets = new LinkedHashMap<>();
        List<ClangLine> lines = DecompilerUtils.toLines(decompileResults.getCCodeMarkup());
        HighFunction highFunction = decompileResults.getHighFunction();
        for (ClangLine line : lines) {
            for (ClangToken token : line.getAllTokens()) {
                if (!(token instanceof ClangVariableToken)) {
                    continue;
                }

                HighSymbol highSymbol = token.getHighSymbol(highFunction);
                if (!isTrackableVariable(highSymbol)) {
                    continue;
                }

                String targetId = buildTargetId(highSymbol);
                VariableTarget target = targets.get(targetId);
                if (target == null) {
                    target = createVariableTarget(targetId, highSymbol);
                    targets.put(targetId, target);
                }
                target.tokens.add(token);
            }
        }
        return targets;
    }

    private VariableTarget createVariableTarget(String targetId, HighSymbol highSymbol) {
        String kind = getTargetKind(highSymbol);
        String currentName = safeTrim(highSymbol.getName());
        String dataType = highSymbol.getDataType() == null ? "" : highSymbol.getDataType().getName();
        String storage = String.valueOf(highSymbol.getStorage());
        String firstUse = highSymbol.getPCAddress() == null ? "entry" : highSymbol.getPCAddress().toString();
        String sourceType = getSourceType(highSymbol);
        boolean autoName = isAutoName(currentName);
        return new VariableTarget(targetId, kind, currentName, dataType, storage, firstUse,
            sourceType, autoName, highSymbol);
    }

    private String getSourceType(HighSymbol highSymbol) {
        Symbol symbol = highSymbol.getSymbol();
        if (symbol != null) {
            return String.valueOf(symbol.getSource());
        }
        return String.valueOf(SourceType.DEFAULT);
    }

    private String buildTargetId(HighSymbol highSymbol) {
        if (highSymbol.isParameter()) {
            return "parameter:" + highSymbol.getCategoryIndex();
        }
        if (highSymbol.isGlobal()) {
            return "global:" + highSymbol.getStorage().getFirstVarnode().getAddress();
        }
        String pcAddress = highSymbol.getPCAddress() == null ? "entry" : highSymbol.getPCAddress().toString();
        return "local:" + highSymbol.getStorage() + "@" + pcAddress;
    }

    private String getTargetKind(HighSymbol highSymbol) {
        if (highSymbol.isParameter()) {
            return "parameter";
        }
        if (highSymbol.isGlobal()) {
            return "global";
        }
        return "local";
    }

    private boolean isTrackableVariable(HighSymbol highSymbol) {
        if (highSymbol == null) {
            return false;
        }
        if (highSymbol.isThisPointer() || highSymbol.isHiddenReturn()) {
            return false;
        }
        return !safeTrim(highSymbol.getName()).isEmpty();
    }

    private boolean isAutoName(String name) {
        return AUTO_NAME_PATTERN.matcher(safeTrim(name)).matches();
    }

    private List<VariableCandidate> getVariableCandidates() {
        if (variableTargetsById.isEmpty()) {
            return Collections.emptyList();
        }

        List<VariableCandidate> candidates = new ArrayList<>();
        for (VariableTarget target : variableTargetsById.values()) {
            candidates.add(target.toCandidate());
        }
        return candidates;
    }

    private boolean isValidRename(RenameItem item) {
        if (item == null) {
            return false;
        }
        return !safeTrim(item.getOldName()).isEmpty() && !safeTrim(item.getNewName()).isEmpty();
    }

    private String formatRenameResult(RenameResult result) {
        if (result == null) {
            return "No rename suggestion returned.";
        }

        StringBuilder sb = new StringBuilder();
        RenameItem functionRename = result.getFunctionRename();
        sb.append("Suggested function:\n");
        if (isValidRename(functionRename)) {
            sb.append(functionRename.getOldName())
              .append(" -> ")
              .append(functionRename.getNewName())
              .append("\nWhy: ")
              .append(safeTrim(functionRename.getExplanation()))
              .append("\n");
        }
        else {
            sb.append("<none>\n");
        }

        sb.append("\nVariables:\n");
        List<RenameItem> variableRenames = result.getVariableRenames();
        if (variableRenames == null || variableRenames.isEmpty()) {
            sb.append("<none>\n");
        }
        else {
            boolean hasAny = false;
            for (RenameItem item : variableRenames) {
                VariableTarget target = variableTargetsById.get(item.getTargetId());
                if (!isApplicableVariableRename(item) || target == null) {
                    continue;
                }
                hasAny = true;
                sb.append(item.getOldName())
                  .append(" -> ")
                  .append(item.getNewName())
                  .append(" [")
                  .append(target.kind)
                  .append("]\nWhy: ")
                  .append(safeTrim(item.getExplanation()))
                  .append("\n\n");
            }
            if (!hasAny) {
                sb.append("<none>\n");
            }
        }

        sb.append("Summary:\n")
          .append(safeTrim(result.getSummary()));
        return sb.toString();
    }

    private String formatSafetyResult(MemorySafetyResult safetyResult) {
        if (safetyResult == null) {
            return "No memory safety result returned.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Overall Assessment:\n")
          .append(safeTrim(safetyResult.getOverallAssessment()))
          .append("\n\nIssues:\n");

        if (safetyResult.getIssues() == null || safetyResult.getIssues().isEmpty()) {
            sb.append("No memory safety issues detected.");
            return sb.toString();
        }

        for (MemorySafetyIssue issue : safetyResult.getIssues()) {
            sb.append("[")
              .append(issue.getSeverity() != null ? issue.getSeverity().toUpperCase() : "UNKNOWN")
              .append("] ")
              .append(issue.getIssueType())
              .append(" @ ")
              .append(issue.getLocation())
              .append("\n")
              .append(issue.getDescription())
              .append("\nSuggestion: ")
              .append(issue.getSuggestion())
              .append("\n\n");
        }
        return sb.toString();
    }

    private RenameSpan getRenameSpanAt(int offset) {
        for (RenameSpan span : renameSpans) {
            if (offset >= span.start && offset < span.end) {
                return span;
            }
        }
        return null;
    }

    private String buildTooltip(RenameItem item) {
        String kind = safeTrim(item.getKind());
        String oldName = escapeHtml(safeTrim(item.getOldName()));
        String newName = escapeHtml(safeTrim(item.getNewName()));
        String explanation = escapeHtml(safeTrim(item.getExplanation()));
        String label = kind.isEmpty() ? oldName + " -> " + newName :
            kind + ": " + oldName + " -> " + newName;
        return "<html><b>" + label + "</b><br/>" + explanation + "</html>";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>");
    }

    private class AiAnalysisTask extends ghidra.util.task.Task {
        private final String taskType;

        AiAnalysisTask(String taskType) {
            super("AI Analysis...", true, false, true);
            this.taskType = taskType;
        }

        @Override
        public void run(TaskMonitor monitor) {
            if (currentFunction == null || loadedCode.isBlank()) {
                SwingUtilities.invokeLater(() -> resultArea.setText(
                    "Load a decompiled function before running AI analysis."));
                return;
            }

            try {
                if ("rename".equals(taskType)) {
                    lastRenameResult = aiService.performRename(loadedCode, currentFunction.getName(),
                        getVariableCandidates());
                    SwingUtilities.invokeLater(this::displayRenameResult);
                }
                else if ("memory_safety".equals(taskType)) {
                    lastSafetyResult = aiService.performMemorySafetyAnalysis(loadedCode,
                        currentFunction.getName());
                    SwingUtilities.invokeLater(this::displaySafetyResult);
                }
            }
            catch (Exception ex) {
                Msg.showError(this, mainPanel, "AI Explainable-Decompiler",
                    "Error: " + ex.getMessage(), ex);
            }
        }

        private void displayRenameResult() {
            PreviewData previewData = buildRenamePreview(lastRenameResult);
            setCodePreview(previewData.text, previewData.spans);
            resultArea.setText(formatRenameResult(lastRenameResult));
            applyRenameBtn.setEnabled(hasApplicableRenames());
        }

        private void displaySafetyResult() {
            setCodePreview(loadedCode, Collections.emptyList());
            resultArea.setText(formatSafetyResult(lastSafetyResult));
            applyRenameBtn.setEnabled(hasApplicableRenames());
        }
    }

    private class PreviewTextPane extends JTextPane {
        @Override
        public String getToolTipText(MouseEvent event) {
            int offset = viewToModel2D(event.getPoint());
            RenameSpan span = getRenameSpanAt(offset);
            if (span == null) {
                return null;
            }
            return buildTooltip(span.item);
        }
    }

    private static class PreviewData {
        private final String text;
        private final List<RenameSpan> spans;

        private PreviewData(String text, List<RenameSpan> spans) {
            this.text = text;
            this.spans = spans;
        }
    }

    private static class RenameSpan {
        private final int start;
        private final int end;
        private final RenameItem item;

        private RenameSpan(int start, int end, RenameItem item) {
            this.start = start;
            this.end = end;
            this.item = item;
        }
    }

    private static class VariableTarget {
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

        private VariableTarget(String targetId, String kind, String currentName, String dataType,
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

        private VariableCandidate toCandidate() {
            return new VariableCandidate(targetId, kind, currentName, dataType, storage, firstUse,
                sourceType, autoName, tokens.size());
        }
    }
}

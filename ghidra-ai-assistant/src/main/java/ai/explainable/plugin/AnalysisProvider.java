package ai.explainable.plugin;

import ai.explainable.backend.BackendClient;
import ai.explainable.backend.HttpBackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.components.crypto.CryptoAnalysisComponent;
import ai.explainable.components.deobfuscation.DeobfuscationComponent;
import ai.explainable.components.memory.MemorySafetyComponent;
import ai.explainable.components.rename.RenameComponent;
import ai.explainable.decompiler.DecompileHelper;
import docking.widgets.OptionDialog;
import ghidra.app.decompiler.ClangLine;
import ghidra.app.decompiler.ClangToken;
import ghidra.app.decompiler.ClangVariableToken;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.component.DecompilerUtils;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.HighSymbol;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import ghidra.util.task.TaskLauncher;
import ghidra.util.task.TaskMonitor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
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
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalysisProvider extends ComponentProviderAdapter implements AnalysisView {
    private static final Color HIGHLIGHT_BG = new Color(255, 243, 176);
    private static final Color HIGHLIGHT_FG = new Color(102, 60, 0);

    private final AIExplainablePlugin plugin;
    private final BackendClient backendClient;
    private final ComponentRegistry registry = new ComponentRegistry();

    private JPanel mainPanel;
    private PreviewTextPane codeArea;
    private JTextArea resultArea;
    private JButton loadButton;
    private JComboBox<ComponentItem> componentCombo;
    private JButton runButton;
    private JButton applyButton;

    private AnalysisContext currentContext;
    private AnalysisComponent<?> lastComponent;
    private Object lastResult;
    private List<HighlightSpan> currentSpans = Collections.emptyList();

    public AnalysisProvider(AIExplainablePlugin plugin, PluginTool tool) {
        super(tool, "AI Explainable-Decompiler", plugin.getName(), Program.class);
        this.plugin = plugin;
        this.backendClient = new HttpBackendClient("http://127.0.0.1:8000");
        registerDefaultComponents();
        setTitle("AI Explainable-Decompiler");
        buildUi();
    }

    private JLabel createLogoLabel() {
        URL logoUrl = getClass().getResource("/images/k2think.png");
        if (logoUrl == null) {
            Msg.showWarn(this, mainPanel, "Logo Missing",
                "Could not load /images/k2think.png from resources.");
            return new JLabel();
        }

        ImageIcon icon = new ImageIcon(logoUrl);
        Image scaled = icon.getImage().getScaledInstance(140, 40, Image.SCALE_SMOOTH);

        JLabel label = new JLabel(new ImageIcon(scaled));
        label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        return label;
    }

    private void registerDefaultComponents() {
        registry.register(new RenameComponent());
        registry.register(new CryptoAnalysisComponent());
        registry.register(new DeobfuscationComponent());
        registry.register(new MemorySafetyComponent());
    }

    private void buildUi() {
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

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(codeArea),
            new JScrollPane(resultArea)
        );
        splitPane.setResizeWeight(0.70);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        loadButton = new JButton("Load Current Decompilation");
        componentCombo = new JComboBox<>(buildComponentModel());
        runButton = new JButton("Run Analysis");
        applyButton = new JButton("Apply Result");
        applyButton.setEnabled(false);

        loadButton.addActionListener(e -> loadCurrent());
        runButton.addActionListener(e -> new TaskLauncher(new ComponentAnalysisTask(getSelectedComponent())));
        applyButton.addActionListener(e -> applyCurrentResult());

        JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftToolbar.add(loadButton);
        leftToolbar.add(componentCombo);
        leftToolbar.add(runButton);
        leftToolbar.add(applyButton);

        JLabel logoLabel = createLogoLabel();

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        header.add(leftToolbar);
        header.add(Box.createHorizontalGlue());
        header.add(logoLabel);

        mainPanel.add(header, BorderLayout.NORTH);

        showCodePreview("No active decompilation loaded.", Collections.emptyList());
        showResultText("Load a function from the current cursor location to begin.");
    }

    private DefaultComboBoxModel<ComponentItem> buildComponentModel() {
        DefaultComboBoxModel<ComponentItem> model = new DefaultComboBoxModel<>();
        for (AnalysisComponent<?> component : registry.all()) {
            model.addElement(new ComponentItem(component));
        }
        return model;
    }

    private AnalysisComponent<?> getSelectedComponent() {
        ComponentItem item = (ComponentItem) componentCombo.getSelectedItem();
        return item == null ? null : item.component();
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    private void loadCurrent() {
        clearResultState();
        Program program = plugin.getCurrentProgram();
        ProgramLocation location = plugin.getProgramLocation();
        if (program == null || location == null) {
            currentContext = null;
            showCodePreview("No active program location.", Collections.emptyList());
            showResultText("Load a function from the current cursor location to begin.");
            return;
        }

        Function function = program.getFunctionManager().getFunctionContaining(location.getAddress());
        if (function == null) {
            currentContext = null;
            showCodePreview("No function at current location.", Collections.emptyList());
            showResultText("Move the cursor into a function and load the current decompilation.");
            return;
        }

        DecompileResults results = DecompileHelper.decompile(function, program, TaskMonitor.DUMMY);
        if (!results.decompileCompleted() || results.getDecompiledFunction() == null) {
            currentContext = null;
            showCodePreview("Decompile failed: " + results.getErrorMessage(), Collections.emptyList());
            showResultText("Decompiler did not return C for the current function.");
            return;
        }

        String decompiledCode = results.getDecompiledFunction().getC();
        HighFunction highFunction = results.getHighFunction();
        Map<String, AnalysisContext.VariableTarget> targets = buildVariableTargets(results);
        currentContext = new AnalysisContext(program, function, decompiledCode, results, highFunction, targets);
        showCodePreview(decompiledCode, Collections.emptyList());
        showResultText(buildLoadMessage(function, targets));
    }

    private Map<String, AnalysisContext.VariableTarget> buildVariableTargets(DecompileResults decompileResults) {
        if (decompileResults == null || decompileResults.getCCodeMarkup() == null ||
                decompileResults.getHighFunction() == null) {
            return Collections.emptyMap();
        }

        Map<String, AnalysisContext.VariableTarget> targets = new LinkedHashMap<>();
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
                AnalysisContext.VariableTarget target = targets.get(targetId);
                if (target == null) {
                    target = createVariableTarget(targetId, highSymbol);
                    targets.put(targetId, target);
                }
                target.getTokens().add(token);
            }
        }
        return targets;
    }

    private AnalysisContext.VariableTarget createVariableTarget(String targetId, HighSymbol highSymbol) {
        String kind = getTargetKind(highSymbol);
        String currentName = AnalysisContext.safeTrim(highSymbol.getName());
        String dataType = highSymbol.getDataType() == null ? "" : highSymbol.getDataType().getName();
        String storage = String.valueOf(highSymbol.getStorage());
        String firstUse = highSymbol.getPCAddress() == null ? "entry" : highSymbol.getPCAddress().toString();
        String sourceType = getSourceType(highSymbol);
        boolean autoName = AnalysisContext.isAutoName(currentName);

        return new AnalysisContext.VariableTarget(
            targetId,
            kind,
            currentName,
            dataType,
            storage,
            firstUse,
            sourceType,
            autoName,
            highSymbol
        );
    }

    private String getSourceType(HighSymbol highSymbol) {
        Symbol symbol = highSymbol.getSymbol();
        if (symbol != null) {
            return String.valueOf(symbol.getSource());
        }
        return String.valueOf(SourceType.DEFAULT);
    }

    private boolean isTrackableVariable(HighSymbol highSymbol) {
        if (highSymbol == null) {
            return false;
        }
        if (highSymbol.isThisPointer() || highSymbol.isHiddenReturn()) {
            return false;
        }
        return !AnalysisContext.safeTrim(highSymbol.getName()).isEmpty();
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

    private void clearResultState() {
        lastComponent = null;
        lastResult = null;
        setApplyEnabled(false);
    }

    private String buildLoadMessage(Function function, Map<String, AnalysisContext.VariableTarget> targets) {
        return "Loaded `" + function.getName() + "`.\n\nTracked variables for AI analysis: " + targets.size();
    }

    private void applyCurrentResult() {
        if (currentContext == null || lastComponent == null || lastResult == null) {
            showInfo("AI Explainable-Decompiler", "No applicable result to apply.");
            return;
        }
        applyTyped(lastComponent, lastResult);
        loadCurrent();
    }

    @SuppressWarnings("unchecked")
    private <R> void applyTyped(AnalysisComponent<?> component, Object result) {
        AnalysisComponent<R> typedComponent = (AnalysisComponent<R>) component;
        try {
            typedComponent.apply(currentContext, (R) result, this);
        }
        catch (Exception ex) {
            showError("AI Explainable-Decompiler", "Failed to apply result: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void showCodePreview(String text, List<HighlightSpan> spans) {
        currentSpans = spans;

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
            for (HighlightSpan span : spans) {
                document.setCharacterAttributes(
                    span.start(),
                    span.end() - span.start(),
                    highlight,
                    false
                );
            }
            codeArea.setCaretPosition(0);
        }
        catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to update preview text", ex);
        }
    }

    @Override
    public void showResultText(String text) {
        resultArea.setText(text);
    }

    @Override
    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
    }

    @Override
    public boolean confirm(String title, String message) {
        return OptionDialog.showYesNoDialog(mainPanel, title, message) == OptionDialog.OPTION_ONE;
    }

    @Override
    public void showInfo(String title, String message) {
        Msg.showInfo(this, mainPanel, title, message);
    }

    @Override
    public void showError(String title, String message, Throwable throwable) {
        Msg.showError(this, mainPanel, title, message, throwable);
    }

    private HighlightSpan getHighlightAt(int offset) {
        for (HighlightSpan span : currentSpans) {
            if (offset >= span.start() && offset < span.end()) {
                return span;
            }
        }
        return null;
    }

    private final class PreviewTextPane extends JTextPane {
        @Override
        public String getToolTipText(MouseEvent event) {
            int offset = viewToModel2D(event.getPoint());
            HighlightSpan span = getHighlightAt(offset);
            return span == null ? null : span.tooltip();
        }
    }

    private record ComponentItem(AnalysisComponent<?> component) {
        @Override
        public String toString() {
            return component.getDisplayName();
        }
    }

    private final class ComponentAnalysisTask extends ghidra.util.task.Task {
        private final AnalysisComponent<?> component;

        private ComponentAnalysisTask(AnalysisComponent<?> component) {
            super("AI Analysis...", true, false, true);
            this.component = component;
        }

        @Override
        public void run(TaskMonitor monitor) {
            if (component == null) {
                SwingUtilities.invokeLater(() ->
                    showResultText("Select an analysis component first."));
                return;
            }

            if (currentContext == null || currentContext.getDecompiledCode().isBlank()) {
                SwingUtilities.invokeLater(() ->
                    showResultText("Load a decompiled function before running AI analysis."));
                return;
            }

            try {
                Object result = analyzeUntyped(component, currentContext);
                SwingUtilities.invokeLater(() -> renderUntyped(component, result));
            }
            catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    showError("AI Explainable-Decompiler", "Error: " + ex.getMessage(), ex));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R analyzeUntyped(AnalysisComponent<?> component, AnalysisContext context) throws Exception {
        AnalysisComponent<R> typed = (AnalysisComponent<R>) component;
        return typed.analyze(context, backendClient);
    }

    @SuppressWarnings("unchecked")
    private <R> void renderUntyped(AnalysisComponent<?> component, Object result) {
        AnalysisComponent<R> typed = (AnalysisComponent<R>) component;
        R typedResult = (R) result;
        lastComponent = typed;
        lastResult = typedResult;
        typed.renderResult(currentContext, typedResult, this);
    }
}
package ai.explainable.plugin;

import ai.explainable.backend.BackendClient;
import ai.explainable.backend.HttpBackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.components.crypto.CryptoAnalysisComponent;
import ai.explainable.components.deobfuscation.DeobfuscationComponent;
import ai.explainable.components.memory.MemorySafetyComponent;
import ai.explainable.components.rename.RenameComponent;
import ai.explainable.decompiler.DecompileHelper;
import com.google.gson.JsonObject;
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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class AnalysisProvider
    extends ComponentProviderAdapter
    implements AnalysisView
{

    private static final Color HIGHLIGHT_BG = new Color(255, 243, 176);
    private static final String RESULT_CARD_TEXT = "text";
    private static final String RESULT_CARD_TABLE = "table";

    private final AIExplainablePlugin plugin;
    private final BackendClient backendClient;
    private final ComponentRegistry registry = new ComponentRegistry();
    private final String snapshotDir;

    private JPanel mainPanel;
    private PreviewCodeArea codeArea;
    private JTextArea resultArea;
    private JPanel resultCardPanel;
    private CardLayout resultCardLayout;
    private JTextArea tableSummaryArea;
    private JTable resultTable;
    private DefaultTableModel resultTableModel;
    private JButton loadButton;
    private JComboBox<ComponentItem> componentCombo;
    private JButton runButton;
    private JButton applyButton;
    private Path currentSnapshotPath;

    private AnalysisContext currentContext;
    private AnalysisComponent<?> lastComponent;
    private Object lastResult;
    private List<HighlightSpan> currentSpans = Collections.emptyList();

    public AnalysisProvider(
        AIExplainablePlugin plugin,
        PluginTool tool,
        String snapshotDir
    ) {
        super(
            tool,
            "AI Explainable-Decompiler",
            plugin.getName(),
            Program.class
        );
        this.plugin = plugin;
        this.snapshotDir = snapshotDir;
        this.backendClient = new HttpBackendClient("http://127.0.0.1:8000");
        registerDefaultComponents();
        setTitle("AI Explainable-Decompiler");
        buildUi();
    }

    private JLabel createLogoLabel() {
        URL logoUrl = getClass().getResource("/images/k2think.png");
        if (logoUrl == null) {
            Msg.showWarn(
                this,
                mainPanel,
                "Logo Missing",
                "Could not load /images/k2think.png from resources."
            );
            return new JLabel();
        }

        ImageIcon icon = new ImageIcon(logoUrl);
        Image scaled = icon
            .getImage()
            .getScaledInstance(140, 40, Image.SCALE_SMOOTH);

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

        codeArea = new PreviewCodeArea();
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setBracketMatchingEnabled(true);
        codeArea.setAnimateBracketMatching(false);
        codeArea.setHighlightCurrentLine(true);
        codeArea.setFadeCurrentLineHighlight(true);
        codeArea.setCurrentLineHighlightColor(new Color(245, 248, 255));
        codeArea.setEditable(false);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setToolTipText(" ");
        codeArea.setMarkOccurrences(false);
        codeArea.setWhitespaceVisible(false);
        codeArea.setLineWrap(false);

        RTextScrollPane codeScrollPane = new RTextScrollPane(codeArea);
        codeScrollPane.setLineNumbersEnabled(true);
        codeScrollPane.setFoldIndicatorEnabled(true);

        buildResultViews();

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            codeScrollPane,
            resultCardPanel
        );
        splitPane.setResizeWeight(0.70);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        loadButton = new JButton("Load Current Decompilation");
        componentCombo = new JComboBox<>(buildComponentModel());
        runButton = new JButton("Run Analysis");
        applyButton = new JButton("Apply Result");
        applyButton.setEnabled(false);

        loadButton.addActionListener(e -> loadCurrent());
        runButton.addActionListener(e ->
            new TaskLauncher(new ComponentAnalysisTask(getSelectedComponent()))
        );
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

        showCodePreview(
            "No active decompilation loaded.",
            Collections.emptyList()
        );
        showResultText(
            "Load a function from the current cursor location to begin."
        );
    }

    private void buildResultViews() {
        resultArea = new JTextArea(12, 60);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        tableSummaryArea = new JTextArea(12, 60);
        tableSummaryArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tableSummaryArea.setEditable(false);
        tableSummaryArea.setLineWrap(true);
        tableSummaryArea.setWrapStyleWord(true);
        tableSummaryArea.setBorder(
            BorderFactory.createTitledBorder("Overall Assessment")
        );
        tableSummaryArea.setBackground(resultArea.getBackground());

        resultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setFillsViewportHeight(true);
        resultTable.setRowHeight(28);
        resultTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setDefaultRenderer(
            Object.class,
            new MultiLineTableCellRenderer()
        );
        DefaultTableCellRenderer headerRenderer =
            (DefaultTableCellRenderer) resultTable
                .getTableHeader()
                .getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        tablePanel.add(new JScrollPane(tableSummaryArea), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        resultCardLayout = new CardLayout();
        resultCardPanel = new JPanel(resultCardLayout);
        resultCardPanel.add(new JScrollPane(resultArea), RESULT_CARD_TEXT);
        resultCardPanel.add(tablePanel, RESULT_CARD_TABLE);
    }

    private void configureTableColumns() {
        int[] preferredWidths = new int[] { 50, 90, 140, 220, 420, 320, 320 };
        for (
            int i = 0;
            i < resultTable.getColumnModel().getColumnCount() &&
            i < preferredWidths.length;
            i++
        ) {
            resultTable
                .getColumnModel()
                .getColumn(i)
                .setPreferredWidth(preferredWidths[i]);
        }

        if (resultTable.getColumnModel().getColumnCount() > 1) {
            resultTable
                .getColumnModel()
                .getColumn(1)
                .setCellRenderer(new SeverityCellRenderer());
        }
    }

    private void refreshRowHeights() {
        for (int row = 0; row < resultTable.getRowCount(); row++) {
            int rowHeight = 28;
            for (
                int column = 0;
                column < resultTable.getColumnCount();
                column++
            ) {
                TableCellRenderer renderer = resultTable.getCellRenderer(
                    row,
                    column
                );
                Component component = resultTable.prepareRenderer(
                    renderer,
                    row,
                    column
                );
                rowHeight = Math.max(
                    rowHeight,
                    component.getPreferredSize().height / 4
                );
            }
            resultTable.setRowHeight(row, rowHeight);
        }
    }

    private DefaultComboBoxModel<ComponentItem> buildComponentModel() {
        DefaultComboBoxModel<ComponentItem> model =
            new DefaultComboBoxModel<>();
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
            showCodePreview(
                "No active program location.",
                Collections.emptyList()
            );
            showResultText(
                "Load a function from the current cursor location to begin."
            );
            return;
        }

        Function function = program
            .getFunctionManager()
            .getFunctionContaining(location.getAddress());
        if (function == null) {
            currentContext = null;
            showCodePreview(
                "No function at current location.",
                Collections.emptyList()
            );
            showResultText(
                "Move the cursor into a function and load the current decompilation."
            );
            return;
        }

        DecompileResults results = DecompileHelper.decompile(
            function,
            program,
            TaskMonitor.DUMMY
        );
        if (
            !results.decompileCompleted() ||
            results.getDecompiledFunction() == null
        ) {
            currentContext = null;
            showCodePreview(
                "Decompile failed: " + results.getErrorMessage(),
                Collections.emptyList()
            );
            showResultText(
                "Decompiler did not return C for the current function."
            );
            return;
        }

        String decompiledCode = results.getDecompiledFunction().getC();
        HighFunction highFunction = results.getHighFunction();
        Map<String, AnalysisContext.VariableTarget> targets =
            buildVariableTargets(results);
        currentContext = new AnalysisContext(
            program,
            function,
            decompiledCode,
            results,
            highFunction,
            targets
        );
        showCodePreview(decompiledCode, Collections.emptyList());
        showResultText(buildLoadMessage(function, targets));

        // ── snapshot ──────────────────────────────────────────────────────────────
        try {
            JsonObject snapshot = DecompileHelper.buildSnapshot(
                program,
                function,
                TaskMonitor.DUMMY
            );
            currentSnapshotPath = DecompileHelper.saveSnapshot(
                snapshot,
                snapshotDir
            );
            currentContext.setSnapshotPath(currentSnapshotPath.toString());
        } catch (Exception ex) {
            currentSnapshotPath = null;
            Msg.showWarn(
                this,
                mainPanel,
                "Snapshot Warning",
                "Snapshot could not be saved: " + ex.getMessage()
            );
        }
        // ─────────────────────────────────────────────────────────────────────────
    }

    private Map<String, AnalysisContext.VariableTarget> buildVariableTargets(
        DecompileResults decompileResults
    ) {
        if (
            decompileResults == null ||
            decompileResults.getCCodeMarkup() == null ||
            decompileResults.getHighFunction() == null
        ) {
            return Collections.emptyMap();
        }

        Map<String, AnalysisContext.VariableTarget> targets =
            new LinkedHashMap<>();
        List<ClangLine> lines = DecompilerUtils.toLines(
            decompileResults.getCCodeMarkup()
        );
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

    private AnalysisContext.VariableTarget createVariableTarget(
        String targetId,
        HighSymbol highSymbol
    ) {
        String kind = getTargetKind(highSymbol);
        String currentName = AnalysisContext.safeTrim(highSymbol.getName());
        String dataType =
            highSymbol.getDataType() == null
                ? ""
                : highSymbol.getDataType().getName();
        String storage = String.valueOf(highSymbol.getStorage());
        String firstUse =
            highSymbol.getPCAddress() == null
                ? "entry"
                : highSymbol.getPCAddress().toString();
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
            return (
                "global:" +
                highSymbol.getStorage().getFirstVarnode().getAddress()
            );
        }
        String pcAddress =
            highSymbol.getPCAddress() == null
                ? "entry"
                : highSymbol.getPCAddress().toString();
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

    private String buildLoadMessage(
        Function function,
        Map<String, AnalysisContext.VariableTarget> targets
    ) {
        return (
            "Loaded `" +
            function.getName() +
            "`.\n\nTracked variables for AI analysis: " +
            targets.size()
        );
    }

    private void applyCurrentResult() {
        if (
            currentContext == null ||
            lastComponent == null ||
            lastResult == null
        ) {
            showInfo(
                "AI Explainable-Decompiler",
                "No applicable result to apply."
            );
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
        } catch (Exception ex) {
            showError(
                "AI Explainable-Decompiler",
                "Failed to apply result: " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public void showCodePreview(String text, List<HighlightSpan> spans) {
        currentSpans = spans == null ? Collections.emptyList() : spans;
        codeArea.setText(text == null ? "" : text);
        codeArea.setCaretPosition(0);

        Highlighter highlighter = codeArea.getHighlighter();
        highlighter.removeAllHighlights();

        if (text == null || currentSpans.isEmpty()) {
            return;
        }

        Highlighter.HighlightPainter painter =
            new DefaultHighlighter.DefaultHighlightPainter(HIGHLIGHT_BG);
        for (HighlightSpan span : currentSpans) {
            try {
                highlighter.addHighlight(span.start(), span.end(), painter);
            } catch (Exception ignored) {
                // Ignore malformed spans to avoid breaking the preview.
            }
        }
    }

    @Override
    public void showResultText(String text) {
        resultArea.setText(text);
        resultCardLayout.show(resultCardPanel, RESULT_CARD_TEXT);
    }

    @Override
    public void showResultTable(
        String summary,
        List<String> columns,
        List<List<String>> rows
    ) {
        tableSummaryArea.setText(summary == null ? "" : summary);

        resultTableModel.setRowCount(0);
        resultTableModel.setColumnCount(0);
        for (String column : columns) {
            resultTableModel.addColumn(column);
        }
        for (List<String> row : rows) {
            resultTableModel.addRow(row.toArray(new Object[0]));
        }
        configureTableColumns();
        refreshRowHeights();
        resultCardLayout.show(resultCardPanel, RESULT_CARD_TABLE);
    }

    @Override
    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
    }

    @Override
    public boolean confirm(String title, String message) {
        return (
            OptionDialog.showYesNoDialog(mainPanel, title, message) ==
            OptionDialog.OPTION_ONE
        );
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

    private final class PreviewCodeArea extends RSyntaxTextArea {

        @Override
        public String getToolTipText(MouseEvent event) {
            int offset = viewToModel2D(event.getPoint());
            HighlightSpan span = getHighlightAt(offset);
            return span == null ? null : span.tooltip();
        }
    }

    private static final class MultiLineTableCellRenderer
        extends JTextArea
        implements TableCellRenderer
    {

        private MultiLineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            setText(value == null ? "" : value.toString());
            setSize(
                table.getColumnModel().getColumn(column).getWidth(),
                Short.MAX_VALUE
            );

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }

            return this;
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
                    showResultText("Select an analysis component first.")
                );
                return;
            }
            if (
                currentContext == null ||
                currentContext.getDecompiledCode().isBlank()
            ) {
                SwingUtilities.invokeLater(() ->
                    showResultText(
                        "Load a decompiled function before running AI analysis."
                    )
                );
                return;
            }

            try {
                Object result = analyzeUntyped(component, currentContext);
                SwingUtilities.invokeLater(() ->
                    renderUntyped(component, result)
                );
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    showError(
                        "AI Explainable-Decompiler",
                        "Error: " + ex.getMessage(),
                        ex
                    )
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R analyzeUntyped(
        AnalysisComponent<?> component,
        AnalysisContext context
    ) throws Exception {
        AnalysisComponent<R> typed = (AnalysisComponent<R>) component;
        return typed.analyze(context, backendClient);
    }

    @SuppressWarnings("unchecked")
    private <R> void renderUntyped(
        AnalysisComponent<?> component,
        Object result
    ) {
        AnalysisComponent<R> typed = (AnalysisComponent<R>) component;
        R typedResult = (R) result;
        lastComponent = typed;
        lastResult = typedResult;
        typed.renderResult(currentContext, typedResult, this);
    }
}

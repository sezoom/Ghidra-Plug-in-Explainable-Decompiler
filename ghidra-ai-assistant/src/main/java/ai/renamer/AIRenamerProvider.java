package ai.renamer;

import ghidra.app.decompiler.DecompileResults;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import ghidra.util.Msg;
import ghidra.util.task.TaskLauncher;
import ghidra.util.task.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class AIRenamerProvider extends ComponentProviderAdapter {
    private final AIRenamerPlugin plugin;
    private final PluginTool tool;
    private final AiService aiService = new HttpAiService("http://127.0.0.1:8000");

    private JPanel mainPanel;
    private JTextArea codeArea;
    private JTextArea resultArea;
    private JButton loadBtn, renameBtn, safetyBtn;

    private Program currentProgram;
    private Function currentFunction;

    private RenameResult lastRenameResult;
    private MemorySafetyResult lastSafetyResult;

    public AIRenamerProvider(AIRenamerPlugin plugin, PluginTool tool) {
        super(tool, "AI Explainable-Decompiler", plugin.getName(), Program.class);
        this.plugin = plugin;
        this.tool = tool;
        setTitle("AI Explainable-Decompiler");
        buildUI();
    }

    private void buildUI() {
        mainPanel = new JPanel(new BorderLayout());

        codeArea = new JTextArea();
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeArea.setEditable(false);
        mainPanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

        resultArea = new JTextArea(12, 60);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setEditable(false);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        JPanel toolbar = new JPanel();
        loadBtn = new JButton("Load Current Decompilation");
        renameBtn = new JButton("AI Rename");
        safetyBtn = new JButton("Memory Safety Analysis");

        toolbar.add(loadBtn);
        toolbar.add(renameBtn);
        toolbar.add(safetyBtn);
        mainPanel.add(toolbar, BorderLayout.NORTH);

        loadBtn.addActionListener(e -> loadCurrent());
        renameBtn.addActionListener(e -> new TaskLauncher(new AiAnalysisTask("rename")));
        safetyBtn.addActionListener(e -> new TaskLauncher(new AiAnalysisTask("memory_safety")));
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    private void loadCurrent() {
        currentProgram = plugin.getCurrentProgram();
        ProgramLocation loc = plugin.getProgramLocation();

        if (currentProgram == null || loc == null) {
            codeArea.setText("No active program location.");
            return;
        }

        currentFunction = currentProgram.getFunctionManager().getFunctionContaining(loc.getAddress());

        if (currentFunction == null) {
            codeArea.setText("No function at current location.");
            return;
        }

        DecompileResults res = DecompileHelper.decompile(currentFunction, currentProgram, TaskMonitor.DUMMY);
        if (res.decompileCompleted() && res.getDecompiledFunction() != null) {
            codeArea.setText(res.getDecompiledFunction().getC());
        }
        else {
            codeArea.setText("Decompile failed: " + res.getErrorMessage());
        }
    }

    private class AiAnalysisTask extends ghidra.util.task.Task {
        private final String taskType;

        public AiAnalysisTask(String taskType) {
            super("AI Analysis...", true, false, true);
            this.taskType = taskType;
        }

        @Override
        public void run(TaskMonitor monitor) {
            if (currentFunction == null || codeArea.getText().isBlank()) {
                return;
            }

            try {
                if ("rename".equals(taskType)) {
                    lastRenameResult = aiService.performRename(codeArea.getText(), currentFunction.getName());
                    SwingUtilities.invokeLater(this::displayRenameResult);
                }
                else if ("memory_safety".equals(taskType)) {
                    lastSafetyResult = aiService.performMemorySafetyAnalysis(codeArea.getText(), currentFunction.getName());
                    SwingUtilities.invokeLater(this::displaySafetyResult);
                }
            }
            catch (Exception ex) {
                Msg.showError(this, mainPanel, "AI Explainable-Decompiler", "Error: " + ex.getMessage(), ex);
            }
        }

        private void displayRenameResult() {
            String variableRenamesText = formatVariableRenames(
                lastRenameResult != null ? lastRenameResult.getVariableRenames() : null);
            String functionName = lastRenameResult != null ? lastRenameResult.getNewFunctionName() : "<none>";
            String explanation = lastRenameResult != null ? lastRenameResult.getExplanation() : "";

            resultArea.setText("Suggested function: " + functionName +
                "\n\nVariables:\n" + variableRenamesText +
                "\n\nExplanation:\n" + explanation);
        }

        private void displaySafetyResult() {
            StringBuilder sb = new StringBuilder();
            if (lastSafetyResult == null) {
                resultArea.setText("No memory safety result returned.");
                return;
            }

            sb.append("Overall Assessment:\n")
              .append(lastSafetyResult.getOverallAssessment())
              .append("\n\n");
            sb.append("=== ISSUES ===\n");
            if (lastSafetyResult.getIssues() == null || lastSafetyResult.getIssues().isEmpty()) {
                sb.append("No memory safety issues detected.\n");
            }
            else {
                for (MemorySafetyIssue issue : lastSafetyResult.getIssues()) {
                    sb.append("[")
                      .append(issue.getSeverity() != null ? issue.getSeverity().toUpperCase() : "UNKNOWN")
                      .append("] ")
                      .append(issue.getIssueType())
                      .append(" @ ")
                      .append(issue.getLocation())
                      .append("\n")
                      .append(issue.getDescription())
                      .append("\n")
                      .append("Suggestion: ")
                      .append(issue.getSuggestion())
                      .append("\n\n");
                }
            }
            resultArea.setText(sb.toString());
        }

        private String formatVariableRenames(Map<String, String> variableRenames) {
            if (variableRenames == null || variableRenames.isEmpty()) {
                return "<none>";
            }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : variableRenames.entrySet()) {
                sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }
}

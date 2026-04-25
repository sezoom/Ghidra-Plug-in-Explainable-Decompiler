package ai.explainable.components.rename;

import ai.explainable.backend.BackendClient;
import ai.explainable.components.AnalysisComponent;
import ai.explainable.plugin.AnalysisContext;
import ai.explainable.plugin.AnalysisView;
import docking.widgets.OptionDialog;
import ghidra.program.model.pcode.HighFunctionDBUtil;
import ghidra.program.model.symbol.SourceType;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenameComponent implements AnalysisComponent<RenameResult> {

    @Override
    public String getId() {
        return "rename";
    }

    @Override
    public String getDisplayName() {
        return "AI Rename";
    }

    @Override
    public Class<RenameResult> getResultType() {
        return RenameResult.class;
    }

    @Override
    public RenameResult analyze(AnalysisContext context, BackendClient client)
        throws Exception {
        RenameRequest request = new RenameRequest(
            context.getDecompiledCode(),
            context.getFunction().getName(),
            buildVariableCandidates(context),
            context.getSnapshotPath()
        );
        return client.analyze(getId(), request, RenameResult.class);
    }

    @Override
    public void renderResult(
        AnalysisContext context,
        RenameResult result,
        AnalysisView view
    ) {
        RenamePreviewBuilder.PreviewData preview = RenamePreviewBuilder.build(
            context,
            result
        );
        view.showCodePreview(preview.text(), preview.spans());
        view.showResultText(formatRenameResult(context, result));
        view.setApplyEnabled(supportsApply(context, result));
    }

    @Override
    public boolean supportsApply(AnalysisContext context, RenameResult result) {
        return (
            RenamePreviewBuilder.getApplicableFunctionRename(context, result) !=
                null ||
            !RenamePreviewBuilder.getApplicableVariableRenames(
                context,
                result
            ).isEmpty()
        );
    }

    @Override
    public void apply(
        AnalysisContext context,
        RenameResult result,
        AnalysisView view
    ) throws Exception {
        RenameItem functionRename =
            RenamePreviewBuilder.getApplicableFunctionRename(context, result);
        List<RenameItem> variableRenames =
            RenamePreviewBuilder.getApplicableVariableRenames(context, result);
        if (
            context.getProgram() == null ||
            context.getFunction() == null ||
            (functionRename == null && variableRenames.isEmpty())
        ) {
            view.showInfo(
                "AI Rename",
                "Load a rename suggestion before applying it."
            );
            return;
        }

        if (
            !view.confirm(
                "Apply AI Renames?",
                buildApplyConfirmationMessage(functionRename, variableRenames)
            )
        ) {
            return;
        }

        int appliedCount = 0;
        List<String> failures = new ArrayList<>();
        int tx = context.getProgram().startTransaction("AI Rename Suggestions");
        boolean commit = false;
        try {
            if (functionRename != null) {
                context
                    .getFunction()
                    .setName(
                        functionRename.getNewName(),
                        SourceType.USER_DEFINED
                    );
                appliedCount++;
            }

            for (RenameItem item : variableRenames) {
                AnalysisContext.VariableTarget target = context
                    .getVariableTargetsById()
                    .get(item.getTargetId());
                if (target == null) {
                    failures.add(
                        item.getOldName() +
                            " -> " +
                            item.getNewName() +
                            " (missing target)"
                    );
                    continue;
                }
                try {
                    HighFunctionDBUtil.updateDBVariable(
                        target.getHighSymbol(),
                        item.getNewName(),
                        null,
                        SourceType.USER_DEFINED
                    );
                    appliedCount++;
                } catch (
                    DuplicateNameException
                    | InvalidInputException
                    | UnsupportedOperationException ex
                ) {
                    failures.add(
                        item.getOldName() +
                            " -> " +
                            item.getNewName() +
                            " (" +
                            ex.getMessage() +
                            ")"
                    );
                }
            }
            commit = appliedCount > 0;
        } finally {
            context.getProgram().endTransaction(tx, commit);
        }

        view.showResultText(
            buildApplySummary(
                appliedCount,
                functionRename,
                variableRenames,
                failures
            )
        );
        if (appliedCount > 0) {
            view.showInfo(
                "AI Rename",
                "Applied " +
                    appliedCount +
                    " rename suggestion(s). Reload the decompilation to refresh context."
            );
        }
    }

    private List<VariableCandidate> buildVariableCandidates(
        AnalysisContext context
    ) {
        if (context.getVariableTargetsById().isEmpty()) {
            return Collections.emptyList();
        }
        List<VariableCandidate> candidates = new ArrayList<>();
        for (AnalysisContext.VariableTarget target : context
            .getVariableTargetsById()
            .values()) {
            candidates.add(
                new VariableCandidate(
                    target.getTargetId(),
                    target.getKind(),
                    target.getCurrentName(),
                    target.getDataType(),
                    target.getStorage(),
                    target.getFirstUse(),
                    target.getSourceType(),
                    target.isAutoName(),
                    target.getTokens().size()
                )
            );
        }
        return candidates;
    }

    private String formatRenameResult(
        AnalysisContext context,
        RenameResult result
    ) {
        if (result == null) {
            return "No rename suggestion returned.";
        }

        StringBuilder sb = new StringBuilder();
        RenameItem functionRename = result.getFunctionRename();
        sb.append("Suggested function:\n");
        if (
            functionRename != null &&
            !safeTrim(functionRename.getOldName()).isEmpty() &&
            !safeTrim(functionRename.getNewName()).isEmpty()
        ) {
            sb
                .append(functionRename.getOldName())
                .append(" -> ")
                .append(functionRename.getNewName())
                .append("\nWhy: ")
                .append(safeTrim(functionRename.getExplanation()))
                .append("\n");
        } else {
            sb.append("<none>\n");
        }

        sb.append("\nVariables:\n");
        List<RenameItem> variableRenames =
            RenamePreviewBuilder.getApplicableVariableRenames(context, result);
        if (variableRenames.isEmpty()) {
            sb.append("<none>\n");
        } else {
            for (RenameItem item : variableRenames) {
                AnalysisContext.VariableTarget target = context
                    .getVariableTargetsById()
                    .get(item.getTargetId());
                if (target == null) {
                    continue;
                }
                sb
                    .append(item.getOldName())
                    .append(" -> ")
                    .append(item.getNewName())
                    .append(" [")
                    .append(target.getKind())
                    .append("]\nWhy: ")
                    .append(safeTrim(item.getExplanation()))
                    .append("\n\n");
            }
        }
        sb.append("Summary:\n").append(safeTrim(result.getSummary()));

        // ── Append control layer output if present ──────────────────────
        if (
            result.getControlOutput() != null &&
            !result.getControlOutput().isBlank()
        ) {
            sb.append("\n").append(result.getControlOutput()).append("\n");
        }
        // ────────────────────────────────────────────────────────────────
        return sb.toString();
    }

    private String buildApplyConfirmationMessage(
        RenameItem functionRename,
        List<RenameItem> variableRenames
    ) {
        StringBuilder sb = new StringBuilder(
            "Apply the following AI rename suggestions?\n\n"
        );
        if (functionRename != null) {
            sb
                .append("Function: ")
                .append(functionRename.getOldName())
                .append(" -> ")
                .append(functionRename.getNewName())
                .append("\n");
        }
        if (!variableRenames.isEmpty()) {
            sb
                .append("Variables: ")
                .append(variableRenames.size())
                .append(" change(s)");
        }
        return sb.toString();
    }

    private String buildApplySummary(
        int appliedCount,
        RenameItem functionRename,
        List<RenameItem> variableRenames,
        List<String> failures
    ) {
        StringBuilder sb = new StringBuilder();
        sb
            .append("Applied rename suggestions: ")
            .append(appliedCount)
            .append("\n");
        if (functionRename != null) {
            sb
                .append("Function: ")
                .append(functionRename.getOldName())
                .append(" -> ")
                .append(functionRename.getNewName())
                .append("\n");
        }
        if (!variableRenames.isEmpty()) {
            sb
                .append("Variables requested: ")
                .append(variableRenames.size())
                .append("\n");
        }
        if (!failures.isEmpty()) {
            sb.append("\nSkipped:\n");
            for (String failure : failures) {
                sb.append("- ").append(failure).append("\n");
            }
        }
        return sb.toString();
    }

    private String safeTrim(String value) {
        return AnalysisContext.safeTrim(value);
    }
}

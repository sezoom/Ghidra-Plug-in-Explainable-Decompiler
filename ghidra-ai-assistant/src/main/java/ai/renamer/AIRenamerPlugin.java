package ai.renamer;

import ghidra.app.CorePluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;

@PluginInfo(
    status = PluginStatus.RELEASED,
    packageName = CorePluginPackage.NAME,
    category = PluginCategoryNames.ANALYSIS,
    shortDescription = "AI Explainable-Decompiler for decompiler-driven rename and memory safety analysis",
    description = "Provides a decompiler-like window that can send the current function to an AI backend for rename suggestions and memory safety analysis."
)
public class AIRenamerPlugin extends ProgramPlugin {
    private final AIRenamerProvider provider;

    public AIRenamerPlugin(PluginTool tool) {
        super(tool);
        provider = new AIRenamerProvider(this, tool);
        tool.addComponentProvider(provider, true);
    }
}

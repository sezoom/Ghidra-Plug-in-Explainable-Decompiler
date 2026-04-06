package ai.explainable.plugin;

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
    shortDescription = "AI Explainable-Decompiler frontend",
    description = "Modular frontend for AI-powered decompiler analyses such as rename suggestions and memory safety analysis."
)
public class AIExplainablePlugin extends ProgramPlugin {
    private final AnalysisProvider provider;

    public AIExplainablePlugin(PluginTool tool) {
        super(tool);
        provider = new AnalysisProvider(this, tool);
        tool.addComponentProvider(provider, true);
    }
}

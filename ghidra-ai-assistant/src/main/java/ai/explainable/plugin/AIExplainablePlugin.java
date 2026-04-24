package ai.explainable.plugin;

import ghidra.app.CorePluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.util.Msg;
import java.io.InputStream;
import java.util.Properties;

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
        String snapshotDir = loadSnapshotDir();
        provider = new AnalysisProvider(this, tool, snapshotDir);
        tool.addComponentProvider(provider, true);
    }

    private String loadSnapshotDir() {
        try {
            java.nio.file.Path extDir = new java.io.File(
                getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            )
                .toPath()
                .getParent()
                .getParent(); // ← extra .getParent() to go above lib/

            java.nio.file.Path propsPath = extDir.resolve(
                "extension.properties"
            );
            Properties props = new Properties();
            props.load(java.nio.file.Files.newInputStream(propsPath));
            return props.getProperty("snapshot.dir", "snapshots");
        } catch (Exception e) {
            Msg.showWarn(
                this,
                null,
                "Snapshot",
                "Could not load extension.properties: " + e.getMessage()
            );
            return "snapshots";
        }
    }
}

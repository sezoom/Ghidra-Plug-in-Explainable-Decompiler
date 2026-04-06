package ai.explainable.plugin;

import ai.explainable.components.AnalysisComponent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComponentRegistry {
    private final Map<String, AnalysisComponent<?>> components = new LinkedHashMap<>();

    public void register(AnalysisComponent<?> component) {
        components.put(component.getId(), component);
    }

    public AnalysisComponent<?> get(String id) {
        return components.get(id);
    }

    public Collection<AnalysisComponent<?>> all() {
        return components.values();
    }
}

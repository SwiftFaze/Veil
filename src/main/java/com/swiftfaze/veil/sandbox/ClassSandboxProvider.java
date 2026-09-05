package com.swiftfaze.veil.sandbox;

import javax.swing.JComponent;
import java.util.List;

/**
 * Exposes every player class as its own searchable dev-console entry -
 * behavior unchanged from before the sandbox was generalized into a
 * provider framework, just reachable directly by class name instead of via
 * an intermediate "Classes" browse screen.
 */
public class ClassSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Classes";

    @Override
    public List<DevConsoleEntry> entries() {
        ClassSandboxModel model = new ClassSandboxModel();
        return model.classNames().stream()
                .map(name -> new DevConsoleEntry(namespaceOf(model.idFor(name)), CATEGORY, name))
                .toList();
    }

    @Override
    public JComponent createPanel(String entryName) {
        return new ClassDetailPanel(new ClassSandboxModel(), entryName);
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }
}

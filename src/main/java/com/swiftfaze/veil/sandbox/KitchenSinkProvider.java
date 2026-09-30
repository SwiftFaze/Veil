package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Supplier;

/**
 * Exposes a single curated kitchen-sink scene containing every tile from
 * ModRegistry.getAllTiles(), with a live preview, movement controls, and
 * a toggleable walkability overlay for testing tile behavior.
 */
public class KitchenSinkProvider implements DevConsoleProvider {

    private static final String NAMESPACE = "sandbox";
    private static final String ID = "sandbox:kitchen-sink";
    private static final String CATEGORY = "Scenes";
    private static final String NAME = "Kitchen Sink";
    private final Supplier<WorldScene> sceneSupplier;

    public KitchenSinkProvider(Supplier<WorldScene> sceneSupplier) {
        this.sceneSupplier = sceneSupplier;
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return List.of(new DevConsoleEntry(NAMESPACE, ID, CATEGORY, NAME));
    }

    @Override
    public JComponent createPanel(String id) {
        return new KitchenSinkPreviewPanel(sceneSupplier.get());
    }
}

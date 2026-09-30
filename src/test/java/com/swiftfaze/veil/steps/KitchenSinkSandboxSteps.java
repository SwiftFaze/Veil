package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.sandbox.KitchenSinkProvider;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.KitchenSinkScene;

import javax.swing.Action;
import javax.swing.JLabel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KitchenSinkSandboxSteps {

    private KitchenSinkPreviewPanel previewPanel;
    private int markerXBeforeMove;
    private int markerYBeforeMove;

    public KitchenSinkSandboxSteps() {
        // Cucumber instantiates with no-arg constructor. Dependencies accessed via SharedScenarioContext.
    }

    @Given("the kitchen-sink entry is opened")
    public void theKitchenSinkEntryIsOpened() {
        // This step assumes the dev console is already running with Kitchen Sink provider
        // Find the kitchen sink entry and open it
        var model = SharedScenarioContext.getDevConsoleSteps().getModel();
        var panel = SharedScenarioContext.getDevConsoleSteps().getPanel();

        var result = model.filteredResults().stream()
                .filter(r -> "sandbox:kitchen-sink".equals(r.entry().id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Kitchen sink entry not found"));

        panel.getSearchField().setText("edit " + result.entry().id());
        panel.runCommand();

        if (panel.isProviderPanelShowing() && panel.getOpenedProviderPanel() instanceof KitchenSinkPreviewPanel) {
            previewPanel = (KitchenSinkPreviewPanel) panel.getOpenedProviderPanel();
            SharedScenarioContext.setKitchenSinkPreviewPanel(previewPanel);
        }
    }

    @Then("the results include exactly one entry from the {string} provider")
    public void theResultsIncludeExactlyOneEntryFromTheProvider(String providerName) {
        var model = SharedScenarioContext.getDevConsoleSteps().getModel();
        assertEquals(1, model.filteredResults().size(),
                "Expected exactly one entry from " + providerName + " provider");
    }

    @Then("that entry has namespace {string}, id {string}, category {string} and name {string}")
    public void thatEntryHasNamespaceIdCategoryAndName(String namespace, String id, String category, String name) {
        var model = SharedScenarioContext.getDevConsoleSteps().getModel();
        assertTrue(model.filteredResults().size() > 0, "No results found");
        var result = model.filteredResults().get(0);
        assertEquals(namespace, result.entry().namespace());
        assertEquals(id, result.entry().id());
        assertEquals(category, result.entry().category());
        assertEquals(name, result.entry().name());
    }

    @Then("the scene contains every tile in the mod registry")
    public void theSceneContainsEveryTileInTheModRegistry() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        java.util.List<com.swiftfaze.veil.world.Tile> allTiles = mods.getAllTiles();

        var scene = previewPanel.getModel().getScene();
        java.util.Set<String> present = new java.util.HashSet<>();
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                var tile = scene.getTile(x, y);
                if (tile != null) {
                    present.add(tile.getId());
                }
            }
        }
        long tilesFound = allTiles.stream().filter(t -> present.contains(t.getId())).count();
        assertEquals((long) allTiles.size(), tilesFound,
                "Not all tiles from registry are in the scene");
    }

    @Then("the marker starts on a walkable tile")
    public void theMarkerStartsOnAWalkableTile() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        boolean walkable = previewPanel.getModel().getScene()
                .isWalkable(previewPanel.getModel().getMarkerX(), previewPanel.getModel().getMarkerY());
        assertTrue(walkable, "Marker does not start on a walkable tile");
    }

    @Given("the marker stands on a walkable tile whose {word} neighbour is walkable")
    public void theMarkerStandsOnAWalkableTileWhoseNeighbourIsWalkable(String direction) {
        assertTrue(previewPanel != null, "Preview panel not shown");

        // Find a tile where the neighbor in the given direction is walkable
        int dx = 0, dy = 0;
        switch (direction.toLowerCase(Locale.ROOT)) {
            case "up" -> dy = -1;
            case "down" -> dy = 1;
            case "left" -> dx = -1;
            case "right" -> dx = 1;
        }

        // Move marker to find a valid position
        for (int testY = 0; testY < previewPanel.getModel().getScene().getHeight(); testY++) {
            for (int testX = 0; testX < previewPanel.getModel().getScene().getWidth(); testX++) {
                if (previewPanel.getModel().getScene().isWalkable(testX, testY)) {
                    int neighborX = testX + dx;
                    int neighborY = testY + dy;
                    if (previewPanel.getModel().getScene().isWalkable(neighborX, neighborY)) {
                        previewPanel.getModel().moveMarker(testX - previewPanel.getModel().getMarkerX(),
                                testY - previewPanel.getModel().getMarkerY());
                        markerXBeforeMove = previewPanel.getModel().getMarkerX();
                        markerYBeforeMove = previewPanel.getModel().getMarkerY();
                        return;
                    }
                }
            }
        }
        throw new AssertionError("Could not find a valid tile with walkable " + direction + " neighbor");
    }

    @When("the key is pressed")
    public void capturePositionBeforeKey() {
        if (previewPanel != null) {
            markerXBeforeMove = previewPanel.getModel().getMarkerX();
            markerYBeforeMove = previewPanel.getModel().getMarkerY();
        }
    }

    @Then("the marker has moved one tile {word}")
    public void theMarkerHasMovedOneDirection(String direction) {
        assertTrue(previewPanel != null, "Preview panel not shown");
        int currentX = previewPanel.getModel().getMarkerX();
        int currentY = previewPanel.getModel().getMarkerY();

        switch (direction.toLowerCase(Locale.ROOT)) {
            case "up" -> assertEquals(markerYBeforeMove - 1, currentY, "Marker did not move up");
            case "down" -> assertEquals(markerYBeforeMove + 1, currentY, "Marker did not move down");
            case "left" -> assertEquals(markerXBeforeMove - 1, currentX, "Marker did not move left");
            case "right" -> assertEquals(markerXBeforeMove + 1, currentX, "Marker did not move right");
        }
    }

    @Given("the marker's {word} neighbour is unwalkable")
    public void theMarkerSNeighbourIsUnwalkable(String direction) {
        assertTrue(previewPanel != null, "Preview panel not shown");
        int[] d = delta(direction);
        var scene = previewPanel.getModel().getScene();
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.isWalkable(x, y) && !scene.isWalkable(x + d[0], y + d[1])) {
                    teleportTo(x, y);
                    return;
                }
            }
        }
        throw new AssertionError("No walkable tile with an unwalkable " + direction + " neighbour");
    }

    private static int[] delta(String direction) {
        return switch (direction.toLowerCase(Locale.ROOT)) {
            case "up" -> new int[]{0, -1};
            case "down" -> new int[]{0, 1};
            case "left" -> new int[]{-1, 0};
            case "right" -> new int[]{1, 0};
            default -> throw new IllegalArgumentException("Unknown direction: " + direction);
        };
    }

    private void teleportTo(int x, int y) {
        var model = previewPanel.getModel();
        model.moveMarker(x - model.getMarkerX(), y - model.getMarkerY());
        markerXBeforeMove = model.getMarkerX();
        markerYBeforeMove = model.getMarkerY();
    }

    @Then("the marker has not moved")
    public void theMarkerHasNotMoved() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertEquals(markerXBeforeMove, previewPanel.getModel().getMarkerX(), "Marker X position changed");
        assertEquals(markerYBeforeMove, previewPanel.getModel().getMarkerY(), "Marker Y position changed");
    }

    @Given("the marker stands on the scene's {word} edge")
    public void theMarkerStandsOnTheScenesEdge(String edge) {
        assertTrue(previewPanel != null, "Preview panel not shown");
        var scene = previewPanel.getModel().getScene();
        int edgeX = "right".equalsIgnoreCase(edge) ? scene.getWidth() - 1 : 0;
        for (int y = 0; y < scene.getHeight(); y++) {
            if (scene.isWalkable(edgeX, y)) {
                teleportTo(edgeX, y);
                return;
            }
        }
        throw new AssertionError("No walkable tile on the " + edge + " edge");
    }

    @Given("the walkability overlay is off")
    public void theWalkabilityOverlayIsOff() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        if (previewPanel.getModel().isOverlayOn()) {
            previewPanel.getModel().toggleOverlay();
        }
        assertFalse(previewPanel.getModel().isOverlayOn(), "Walkability overlay is on, expected off");
    }

    @Given("the walkability overlay is on")
    public void theWalkabilityOverlayIsOn() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        if (!previewPanel.getModel().isOverlayOn()) {
            previewPanel.getModel().toggleOverlay();
        }
        assertTrue(previewPanel.getModel().isOverlayOn(), "Walkability overlay is off, expected on");
    }

    @Then("every cell is marked walkable or unwalkable as WorldScene.isWalkable reports it")
    public void everyCellIsMarkedWalkableOrUnwalkable() {
        var scene = previewPanel.getModel().getScene();
        BufferedImage off = renderWithOverlay(false);
        BufferedImage on = renderWithOverlay(true);
        java.util.Map<Boolean, java.util.Set<Integer>> tintsByWalkability = new java.util.HashMap<>();
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                java.awt.Point p = backgroundPixelIn(off, x, y);
                if (p == null) {
                    continue; // glyph fills the whole cell, so the tint behind it can't show
                }
                assertNotEquals(off.getRGB(p.x, p.y), on.getRGB(p.x, p.y), "Cell " + x + "," + y + " not tinted");
                tintsByWalkability.computeIfAbsent(scene.isWalkable(x, y), k -> new java.util.HashSet<>())
                        .add(on.getRGB(p.x, p.y));
            }
        }
        assertEquals(java.util.Set.of(true, false), tintsByWalkability.keySet(), "Scene needs both kinds of cell");
        assertEquals(1, tintsByWalkability.get(true).size(), "Walkable cells don't share one tint");
        assertEquals(1, tintsByWalkability.get(false).size(), "Unwalkable cells don't share one tint");
        assertNotEquals(tintsByWalkability.get(true), tintsByWalkability.get(false), "Both kinds share a tint");
    }

    @Then("no walkability marking is drawn")
    public void noWalkabilityMarkingIsDrawn() {
        assertFalse(previewPanel.getModel().isOverlayOn(), "Overlay is still on");
        BufferedImage now = render();
        BufferedImage off = renderWithOverlay(false);
        assertEquals(WidgetTheme.BACKGROUND.getRGB(), now.getRGB(1, 1), "A cell is still tinted");
        assertEquals(off.getRGB(1, 1), now.getRGB(1, 1));
    }

    @Given("the marker has moved away from its start position")
    public void theMarkerHasMovedAwayFromItsStartPosition() {
        assertTrue(previewPanel != null, "Preview panel not shown");

        // Move marker away from start position
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int newX = previewPanel.getModel().getMarkerX() + dx;
                int newY = previewPanel.getModel().getMarkerY() + dy;
                if (previewPanel.getModel().getScene().isWalkable(newX, newY)) {
                    previewPanel.getModel().moveMarker(dx, dy);
                    return;
                }
            }
        }
        throw new AssertionError("Could not move marker away from start position");
    }

    @Then("the marker is at its start position")
    public void theMarkerIsAtItsStartPosition() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        // Find the start position (first walkable tile)
        for (int y = 0; y < previewPanel.getModel().getScene().getHeight(); y++) {
            for (int x = 0; x < previewPanel.getModel().getScene().getWidth(); x++) {
                if (previewPanel.getModel().getScene().isWalkable(x, y)) {
                    assertEquals(x, previewPanel.getModel().getMarkerX(), "Marker X not at start");
                    assertEquals(y, previewPanel.getModel().getMarkerY(), "Marker Y not at start");
                    return;
                }
            }
        }
    }

    @Then("the opened detail panel shows {string}")
    public void theOpenedDetailPanelShows(String text) {
        var panel = SharedScenarioContext.getDevConsoleSteps().getPanel();
        assertTrue(panel.isProviderPanelShowing(), "Provider panel not shown");
        boolean found = java.util.Arrays.stream(previewPanel.getComponents())
                .anyMatch(c -> c instanceof JLabel label && text.equals(label.getText()));
        assertTrue(found, "No label reading \"" + text + "\" in the opened panel");
    }

    @Then("no preview is drawn")
    public void noPreviewIsDrawn() {
        assertFalse(previewPanel.isPreviewShown(), "Preview drawn for an empty registry");
    }

    @When("the W key is pressed")
    public void theWKeyIsPressed() {
        // Ensure UiComponentFrameworkSteps is initialized for this test
        if (SharedScenarioContext.getUiSteps() == null) {
            new UiComponentFrameworkSteps();
        }
        SharedScenarioContext.getUiSteps().theKeyIsPressed("W");
    }

    private void ensureUiStepsInitialized() {
        if (SharedScenarioContext.getUiSteps() == null) {
            new UiComponentFrameworkSteps();
        }
    }

    @When("the Up key is pressed")
    public void theUpKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Up");
    }

    @When("the Down key is pressed")
    public void theDownKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Down");
    }

    @When("the Left key is pressed")
    public void theLeftKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Left");
    }

    @When("the Right key is pressed")
    public void theRightKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Right");
    }

    @When("the Z key is pressed")
    public void theZKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Z");
    }

    @When("the S key is pressed")
    public void theSKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("S");
    }

    @When("the Q key is pressed")
    public void theQKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("Q");
    }

    @When("the D key is pressed")
    public void theDKeyIsPressed() {
        ensureUiStepsInitialized();
        SharedScenarioContext.getUiSteps().theKeyIsPressed("D");
    }

    @Given("the mod registry contains no tiles")
    public void theModRegistryContainsNoTiles() {
        KitchenSinkScene empty = KitchenSinkScene.of(java.util.List.of());
        SharedScenarioContext.getDevConsoleSteps().runWith(new KitchenSinkProvider(() -> empty));
    }

    @Then("the marking is a background tint, so each tile's glyph is still drawn")
    public void theMarkingIsABackgroundTint() {
        BufferedImage off = renderWithOverlay(false);
        BufferedImage on = renderWithOverlay(true);
        var scene = previewPanel.getModel().getScene();
        int glyphPixels = 0;
        for (int y = 0; y < scene.getHeight() * GameConst.TILE_HEIGHT; y++) {
            for (int x = 0; x < scene.getWidth() * GameConst.TILE_WIDTH; x++) {
                var tile = scene.getTile(x / GameConst.TILE_WIDTH, y / GameConst.TILE_HEIGHT);
                if (tile != null && off.getRGB(x, y) == tile.getColor().getRGB()) {
                    glyphPixels++;
                    assertEquals(off.getRGB(x, y), on.getRGB(x, y), "Tint covers a glyph pixel at " + x + "," + y);
                }
            }
        }
        assertTrue(glyphPixels > 0, "No glyph pixels found to compare");
    }

    private static java.awt.Point backgroundPixelIn(BufferedImage image, int cellX, int cellY) {
        for (int dy = 0; dy < GameConst.TILE_HEIGHT; dy++) {
            for (int dx = 0; dx < GameConst.TILE_WIDTH; dx++) {
                int px = cellX * GameConst.TILE_WIDTH + dx;
                int py = cellY * GameConst.TILE_HEIGHT + dy;
                if (image.getRGB(px, py) == WidgetTheme.BACKGROUND.getRGB()) {
                    return new java.awt.Point(px, py);
                }
            }
        }
        return null;
    }

    private BufferedImage renderWithOverlay(boolean on) {
        var model = previewPanel.getModel();
        boolean original = model.isOverlayOn();
        if (model.isOverlayOn() != on) {
            model.toggleOverlay();
        }
        BufferedImage image = render();
        if (model.isOverlayOn() != original) {
            model.toggleOverlay();
        }
        return image;
    }

    private BufferedImage render() {
        var scene = previewPanel.getModel().getScene();
        int width = scene.getWidth() * GameConst.TILE_WIDTH;
        int height = scene.getHeight() * GameConst.TILE_HEIGHT;
        previewPanel.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        previewPanel.paint(g);
        g.dispose();
        return image;
    }
}

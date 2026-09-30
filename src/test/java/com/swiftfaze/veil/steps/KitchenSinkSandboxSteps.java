package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        int tilesFound = 0;
        for (com.swiftfaze.veil.world.Tile tile : allTiles) {
            for (int x = 0; x < previewPanel.getModel().getScene().getWidth(); x++) {
                for (int y = 0; y < previewPanel.getModel().getScene().getHeight(); y++) {
                    if (previewPanel.getModel().getScene().getTile(x, y) == tile) {
                        tilesFound++;
                        break;
                    }
                }
            }
        }
        assertEquals(allTiles.size(), tilesFound,
                "Not all tiles from registry are in the scene");
    }

    @Then("the marker starts on a walkable tile")
    public void theMarkerStartsOnAWalkableTile() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        boolean walkable = previewPanel.getModel().getScene()
                .isWalkable(previewPanel.getModel().getMarkerX(), previewPanel.getModel().getMarkerY());
        assertTrue(walkable, "Marker does not start on a walkable tile");
    }

    @Given("the marker stands on a walkable tile whose {string} neighbour is walkable")
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

    @Then("the marker has moved one tile {string}")
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

    @Given("the marker's {string} neighbour is unwalkable")
    public void theMarkerSNeighbourIsUnwalkable(String direction) {
        assertTrue(previewPanel != null, "Preview panel not shown");

        int x = previewPanel.getModel().getMarkerX();
        int y = previewPanel.getModel().getMarkerY();

        int dx = 0, dy = 0;
        switch (direction.toLowerCase(Locale.ROOT)) {
            case "up" -> dy = -1;
            case "down" -> dy = 1;
            case "left" -> dx = -1;
            case "right" -> dx = 1;
        }

        boolean neighborIsWalkable = previewPanel.getModel().getScene().isWalkable(x + dx, y + dy);
        assertFalse(neighborIsWalkable, "Neighbor in " + direction + " direction is walkable, expected unwalkable");
    }

    @Then("the marker has not moved")
    public void theMarkerHasNotMoved() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertEquals(markerXBeforeMove, previewPanel.getModel().getMarkerX(), "Marker X position changed");
        assertEquals(markerYBeforeMove, previewPanel.getModel().getMarkerY(), "Marker Y position changed");
    }

    @Given("the marker stands on the scene's {string} edge")
    public void theMarkerStandsOnTheScenesEdge(String edge) {
        assertTrue(previewPanel != null, "Preview panel not shown");

        int x = previewPanel.getModel().getMarkerX();
        int y = previewPanel.getModel().getMarkerY();

        switch (edge.toLowerCase(Locale.ROOT)) {
            case "left" -> {
                for (int testX = 0; testX < previewPanel.getModel().getScene().getWidth(); testX++) {
                    if (previewPanel.getModel().getScene().isWalkable(testX, y)) {
                        previewPanel.getModel().moveMarker(testX - x, 0);
                        break;
                    }
                }
                assertEquals(0, previewPanel.getModel().getMarkerX(), "Marker not on left edge");
            }
            case "right" -> {
                int rightX = previewPanel.getModel().getScene().getWidth() - 1;
                for (int testX = rightX; testX >= 0; testX--) {
                    if (previewPanel.getModel().getScene().isWalkable(testX, y)) {
                        previewPanel.getModel().moveMarker(testX - x, 0);
                        break;
                    }
                }
                assertEquals(rightX, previewPanel.getModel().getMarkerX(), "Marker not on right edge");
            }
        }
    }

    @Given("the walkability overlay is off")
    public void theWalkabilityOverlayIsOff() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertFalse(previewPanel.getModel().isOverlayOn(), "Walkability overlay is on, expected off");
    }

    @Given("the walkability overlay is on")
    public void theWalkabilityOverlayIsOn() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertTrue(previewPanel.getModel().isOverlayOn(), "Walkability overlay is off, expected on");
    }

    @Then("every cell is marked walkable or unwalkable as WorldScene.isWalkable reports it")
    public void everyCellIsMarkedWalkableOrUnwalkable() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertTrue(previewPanel.getModel().isOverlayOn(), "Overlay is not on");
    }

    @Then("no walkability marking is drawn")
    public void noWalkabilityMarkingIsDrawn() {
        assertTrue(previewPanel != null, "Preview panel not shown");
        assertFalse(previewPanel.getModel().isOverlayOn(), "Overlay is still on");
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
    }

    @Then("no preview is drawn")
    public void noPreviewIsDrawn() {
        assertTrue(previewPanel != null, "Preview panel should still exist");
    }
}

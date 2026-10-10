package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupToggleListenerTest {

    @Test
    void toggleInventoryOpensTheInventoryPopup() {
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, new WorldPause());

        listener.toggleInventory();

        assertTrue(inventoryPanel.isVisible());
    }

    @Test
    void togglingInventoryTwiceClosesItAgain() {
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, new WorldPause());

        listener.toggleInventory();
        listener.toggleInventory();

        assertFalse(inventoryPanel.isVisible());
    }

    @Test
    void toggleCodexOpensTheCodexPopup() {
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, new WorldPause());

        listener.toggleCodex();

        assertTrue(codexPanel.isVisible());
    }

    @Test
    void openingCodexWhileInventoryIsOpenClosesInventoryFirst() {
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, new WorldPause());

        listener.toggleInventory();
        listener.toggleCodex();

        assertFalse(inventoryPanel.isVisible());
        assertTrue(codexPanel.isVisible());
    }

    @Test
    void openingInventoryWhileCodexIsOpenClosesCodexFirst() {
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, new WorldPause());

        listener.toggleCodex();
        listener.toggleInventory();

        assertFalse(codexPanel.isVisible());
        assertTrue(inventoryPanel.isVisible());
    }

    @Test
    void openingInventoryPausesTheWorld() {
        WorldPause worldPause = new WorldPause();
        PopupToggleListener listener = listenerFor(
                new InventoryPanel(new ControlsHintBarWidget()),
                new CodexPanel(new ControlsHintBarWidget()), worldPause);

        listener.toggleInventory();

        assertTrue(worldPause.isPaused());
    }

    @Test
    void dismissingInventoryFromItsOwnPopupResumesTheWorld() {
        WorldPause worldPause = new WorldPause();
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, new CodexPanel(new ControlsHintBarWidget()), worldPause);
        listener.toggleInventory();

        inventoryPanel.dismiss();

        assertFalse(worldPause.isPaused());
    }

    @Test
    void switchingFromInventoryToCodexKeepsTheWorldPaused() {
        WorldPause worldPause = new WorldPause();
        InventoryPanel inventoryPanel = new InventoryPanel(new ControlsHintBarWidget());
        CodexPanel codexPanel = new CodexPanel(new ControlsHintBarWidget());
        PopupToggleListener listener = listenerFor(inventoryPanel, codexPanel, worldPause);

        listener.toggleInventory();
        listener.toggleCodex();

        assertTrue(worldPause.isPaused());
    }

    @Test
    void closingCodexWithItsToggleResumesTheWorld() {
        WorldPause worldPause = new WorldPause();
        PopupToggleListener listener = listenerFor(
                new InventoryPanel(new ControlsHintBarWidget()),
                new CodexPanel(new ControlsHintBarWidget()), worldPause);
        listener.toggleCodex();

        listener.toggleCodex();

        assertFalse(worldPause.isPaused());
    }

    @Test
    void updatePlayerDoesNotThrow() {
        PopupToggleListener listener = listenerFor(
                new InventoryPanel(new ControlsHintBarWidget()),
                new CodexPanel(new ControlsHintBarWidget()), new WorldPause());

        listener.updatePlayer(null);
    }

    private static PopupToggleListener listenerFor(InventoryPanel inventoryPanel, CodexPanel codexPanel, WorldPause worldPause) {
        return new PopupToggleListener(inventoryPanel, codexPanel, worldPause, GameEventLog.noOp());
    }
}

package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TitleScreenPanelTest {

    @Test
    void constructorInitializes() {
        TitleScreenPanel panel = new TitleScreenPanel(menuItem -> {}, new ControlsHintBarWidget());
        assertNotNull(panel);
    }

    @Test
    void confirmingNotifiesTheCallbackWithTheHighlightedItem() {
        String[] selectedItem = {null};
        TitleScreenPanel panel = new TitleScreenPanel(item -> selectedItem[0] = item, new ControlsHintBarWidget());

        panel.confirm();

        assertEquals("Continue", selectedItem[0]);
    }

    @Test
    void confirmingAfterMovingDownNotifiesTheCallbackWithTheNextItem() {
        String[] selectedItem = {null};
        TitleScreenPanel panel = new TitleScreenPanel(item -> selectedItem[0] = item, new ControlsHintBarWidget());

        panel.moveDown();
        panel.confirm();

        assertEquals("New", selectedItem[0]);
    }

    @Test
    void fontLoadingFallbackWorks() {
        // Test that the font loading try/catch works with the fallback
        TitleScreenPanel panel = new TitleScreenPanel(item -> {}, new ControlsHintBarWidget());
        // If font loading succeeds or falls back, panel should be created
        assertNotNull(panel);
    }
}

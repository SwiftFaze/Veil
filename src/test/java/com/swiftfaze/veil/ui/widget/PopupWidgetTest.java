package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupWidgetTest {

    @Test
    void openWithoutFocusManager() {
        PopupWidget popup = new PopupWidget();
        popup.setFocusManager(null);
        popup.open();
        assertTrue(popup.isVisible());
    }

    @Test
    void openWithFocusManager() {
        PopupWidget popup = new PopupWidget();
        FocusManager manager = new FocusManager();
        popup.setFocusManager(manager);
        popup.open();
        assertTrue(popup.isVisible());
    }

    @Test
    void dismissWithoutFocusManager() {
        PopupWidget popup = new PopupWidget();
        popup.setFocusManager(null);
        popup.open();
        popup.dismiss();
        assertFalse(popup.isVisible());
    }

    @Test
    void dismissWithFocusManager() {
        PopupWidget popup = new PopupWidget();
        FocusManager manager = new FocusManager();
        popup.setFocusManager(manager);
        popup.open();
        popup.dismiss();
        assertFalse(popup.isVisible());
    }

    @Test
    void setOnDismissCallbackExecutes() {
        PopupWidget popup = new PopupWidget();
        boolean[] called = {false};
        popup.setOnDismiss(() -> called[0] = true);
        popup.dismiss();
        assertTrue(called[0]);
    }

    @Test
    void isFullScreenReturnsTrue() {
        PopupWidget popup = new PopupWidget();
        assertTrue(popup.isFullScreen());
    }
}

package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenNavigatorTest {
    private final GameEventLog log = GameEventLog.inMemory();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, JComponent> cards = new HashMap<>();
    private final ScreenNavigator navigator = new ScreenNavigator(cardLayout, cardPanel, cards, log);

    ScreenNavigatorTest() {
        for (String name : List.of("title", "settings", "game")) {
            JPanel card = new JPanel();
            cardPanel.add(card, name);
            cards.put(name, card);
        }
    }

    @Test
    void initialShowRecordsNothing() {
        navigator.showInitial("title");
        assertEquals("title", navigator.getCurrentCard());
        assertTrue(log.getEvents().isEmpty());
    }

    @Test
    void eachRealSwitchRecordsOneScreenChanged() {
        navigator.showInitial("title");
        navigator.navigateTo("settings");
        navigator.navigateTo("title");
        assertEquals(List.of(new GameEvent.ScreenChanged("title", "settings"),
                new GameEvent.ScreenChanged("settings", "title")), log.getEvents());
        assertTrue(cards.get("title").isVisible());
    }

    @Test
    void switchingToTheCurrentCardRecordsNothing() {
        navigator.showInitial("game");
        navigator.navigateTo("game");
        assertTrue(log.getEvents().isEmpty());
        assertEquals("game", navigator.getCurrentCard());
    }
}

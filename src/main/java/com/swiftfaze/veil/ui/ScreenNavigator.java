package com.swiftfaze.veil.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Routes all card switches through a single point, tracking the current card and
 * notifying listeners. Testable, holds the current card name, and avoids duplicate
 * events when from==to.
 */
public class ScreenNavigator {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final BiConsumer<String, String> onScreenChange;
    private String currentCard;

    public ScreenNavigator(CardLayout cardLayout, JPanel cardPanel, BiConsumer<String, String> onScreenChange, String initialCard) {
        this.cardLayout = cardLayout;
        this.cardPanel = cardPanel;
        this.onScreenChange = onScreenChange;
        this.currentCard = initialCard;
    }

    /**
     * Navigates to a card by name. Calls the screen change listener if the card
     * actually changes (from != to).
     *
     * @param cardName the name of the card to show
     */
    public void navigateTo(String cardName) {
        if (cardName.equals(currentCard)) {
            return;
        }

        String previousCard = currentCard;
        currentCard = cardName;
        cardLayout.show(cardPanel, cardName);
        onScreenChange.accept(previousCard, cardName);
    }

    /**
     * Returns the name of the currently visible card.
     *
     * @return the current card name
     */
    public String getCurrentCard() {
        return currentCard;
    }
}

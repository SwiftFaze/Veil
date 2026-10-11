package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialoguePanelTest {

    private static final Dialogue GREETING = new Dialogue("test:greeting", "hello", Map.of(
            "hello", new DialogueNode("Well met.", List.of(
                    new DialogueChoice("Who are you?", "who"),
                    new DialogueChoice("Goodbye.", null))),
            "who", new DialogueNode("Nobody special.", List.of(
                    new DialogueChoice("Farewell.", null)))));

    private static final Dialogue NINE_CHOICES = new Dialogue("test:nine", "hello", Map.of(
            "hello", new DialogueNode("Pick.", IntStream.rangeClosed(1, 9)
                    .mapToObj(number -> number == 9 ? new DialogueChoice("Ninth.", null) : new DialogueChoice("Pick " + number, "hello"))
                    .toList())));

    private static DialoguePanel panel() {
        return new DialoguePanel(hints -> { });
    }

    @Test
    void showingAConversationOpensThePanelHeadedByTheNpcName() {
        DialoguePanel panel = panel();

        panel.show(new Conversation("Mira", GREETING));

        assertTrue(panel.isVisible());
        assertEquals("Mira", panel.title());
    }

    @Test
    void theChatLogHoldsTheStartLineAndTheChoicesAreNumberedFromOne() {
        DialoguePanel panel = panel();

        panel.show(new Conversation("Mira", GREETING));

        assertEquals(List.of("Mira: Well met."), panel.logLines());
        assertEquals(List.of("1. Who are you?", "2. Goodbye."), panel.choiceLabels());
    }

    @Test
    void theFirstChoiceStartsHighlightedAndTheHighlightDoesNotWrap() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        panel.onUp();
        assertEquals(1, panel.highlightedChoice());

        panel.onDown();
        panel.onDown();
        assertEquals(2, panel.highlightedChoice());
    }

    @Test
    void enterPicksTheHighlightedChoice() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));
        panel.onDown();

        panel.chooseHighlighted();

        assertEquals(List.of("Mira: Well met.", "You: Goodbye."), panel.logLines());
        assertFalse(panel.isVisible());
    }

    @Test
    void aNumberKeyPicksThatChoiceAndAdvances() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        panel.chooseNumber(1);

        assertEquals(List.of("Mira: Well met.", "You: Who are you?", "Mira: Nobody special."), panel.logLines());
        assertEquals(List.of("1. Farewell."), panel.choiceLabels());
    }

    @Test
    void aNumberKeyThatAdvancesLeavesThePopupOpenOnTheFirstChoice() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        panel.chooseNumber(1);

        assertEquals(1, panel.highlightedChoice());
        assertTrue(panel.isVisible());
    }

    @Test
    void aNumberWithNoMatchingChoiceChangesNothing() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        panel.chooseNumber(3);

        assertEquals(List.of("Mira: Well met."), panel.logLines());
        assertTrue(panel.isVisible());
    }

    @Test
    void reopeningStartsAgainFromTheStartNodeWithAnEmptyLog() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));
        panel.chooseNumber(1);
        panel.dismiss();

        panel.show(new Conversation("Mira", GREETING));

        assertEquals(List.of("Mira: Well met."), panel.logLines());
        assertEquals(List.of("1. Who are you?", "2. Goodbye."), panel.choiceLabels());
    }

    @Test
    void showingPublishesTheConversationHints() {
        List<ControlsHintBarWidget.Hint> published = new ArrayList<>();
        DialoguePanel panel = new DialoguePanel(published::addAll);

        panel.show(new Conversation("Mira", GREETING));

        assertTrue(published.contains(new ControlsHintBarWidget.Hint("escape", "End")));
    }

    @Test
    void theDisplayedLogFollowsTheConversationAsItAdvances() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        panel.chooseNumber(1);

        assertEquals("Mira: Well met.\nYou: Who are you?\nMira: Nobody special.\n", panel.displayedLog());
    }

    @Test
    void theLogScrollsToTheNewestLineOnceLaidOut() throws InterruptedException, InvocationTargetException {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", GREETING));

        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(panel.displayedLog().length(), panel.logCaretPosition());
    }

    @Test
    void theNinthNumberKeyIsBoundToThePopup() {
        DialoguePanel panel = panel();
        panel.show(new Conversation("Mira", NINE_CHOICES));

        Object binding = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_9, 0));
        panel.getActionMap().get(binding).actionPerformed(null);

        assertFalse(panel.isVisible());
    }
}

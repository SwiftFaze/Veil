package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}

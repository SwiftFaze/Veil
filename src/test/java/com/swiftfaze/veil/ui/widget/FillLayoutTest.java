package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FillLayoutTest {

    @Test
    void layoutContainerStretchesEveryChildToTheParentsFullBounds() {
        JPanel parent = new JPanel(new FillLayout());
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        parent.add(first);
        parent.add(second);
        parent.setBounds(0, 0, 300, 200);

        parent.doLayout();

        assertEquals(new Rectangle(0, 0, 300, 200), first.getBounds());
        assertEquals(new Rectangle(0, 0, 300, 200), second.getBounds());
    }
}

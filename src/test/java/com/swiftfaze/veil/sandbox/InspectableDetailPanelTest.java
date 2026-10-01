package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InspectableDetailPanelTest {

    private static final String TITLE = "Dirt";

    @Test
    void showsTheTitleInAHeaderAboveTheDetails() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertInstanceOf(HeaderWidget.class, panel.getComponent(0));
        assertInstanceOf(DetailsPaneWidget.class, panel.getComponent(1));
    }

    @Test
    void headerAndDetailsShareLeftAlignmentSoBoxLayoutKeepsThemInLine() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertEquals(Component.LEFT_ALIGNMENT, panel.getComponent(0).getAlignmentX());
        assertEquals(Component.LEFT_ALIGNMENT, panel.getComponent(1).getAlignmentX());
    }

    private static Inspectable entry() {
        return new Inspectable() {
            @Override
            public String getId() {
                return "core:dirt";
            }

            @Override
            public String getName() {
                return TITLE;
            }

            @Override
            public List<DetailTable> getDetailTables() {
                return List.of(new DetailTable("", List.of("Field", "Value"), List.of(List.of("ID", "core:dirt"))));
            }
        };
    }
}

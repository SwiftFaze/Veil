package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileSandboxProviderTest {

    @Test
    void exposesEveryTileAsAnEntryWithNamespaceAndCategory() {
        TileSandboxProvider provider = new TileSandboxProvider();

        List<DevConsoleEntry> entries = provider.entries();

        Optional<DevConsoleEntry> grass = entries.stream()
                .filter(entry -> entry.name().equals("grass"))
                .findFirst();
        assertTrue(grass.isPresent());
        assertEquals("core", grass.get().namespace());
        assertEquals("core:grass", grass.get().id());
        assertEquals("Tiles", grass.get().category());
    }

    @Test
    void localNameIsDerivedFromIdAfterColon() {
        TileSandboxProvider provider = new TileSandboxProvider();

        Optional<DevConsoleEntry> water = provider.entries().stream()
                .filter(entry -> entry.id().equals("core:water"))
                .findFirst();
        assertTrue(water.isPresent());
        assertEquals("water", water.get().name());
    }

    @Test
    void createsPanelOpenedToTheRequestedTile() {
        TileSandboxProvider provider = new TileSandboxProvider();

        DetailsPaneWidget panel = (DetailsPaneWidget) provider.createPanel("core:grass");

        assertEquals(1, panel.getTableCount());
        TableWidget<List<String>> table = panel.getTable(0);
        table.moveToStart();
        List<String> idRow = table.getSelectedRow();
        assertEquals("ID", idRow.get(0));
        assertEquals("core:grass", idRow.get(1));
    }

    @Test
    void rejectsTileIdThatDoesNotExist() {
        TileSandboxProvider provider = new TileSandboxProvider();

        assertThrows(IllegalArgumentException.class, () -> provider.createPanel("core:no_such_tile"));
    }

    @Test
    void emptyProviderContributesNoResults() {
        TileSandboxProvider provider = new TileSandboxProvider(List.of());

        List<DevConsoleEntry> entries = provider.entries();

        assertTrue(entries.isEmpty());
    }

    @Test
    void panelShowsAllDetailTablesFromTile() {
        FakeTile tile = new FakeTile("test:tile", 'x', new Color(100, 150, 200), true);
        TileSandboxProvider provider = new TileSandboxProvider(List.of(tile));

        DetailsPaneWidget panel = (DetailsPaneWidget) provider.createPanel("test:tile");

        assertEquals(1, panel.getTableCount());
        TableWidget<List<String>> table = panel.getTable(0);
        assertEquals(4, table.getRowCount());
    }

    /**
     * Minimal Inspectable for testing.
     */
    private static class FakeTile implements Inspectable {
        private final String id;
        private final char symbol;
        private final Color color;
        private final boolean walkable;

        FakeTile(String id, char symbol, Color color, boolean walkable) {
            this.id = id;
            this.symbol = symbol;
            this.color = color;
            this.walkable = walkable;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        public List<DetailTable> getDetailTables() {
            List<List<String>> rows = List.of(
                    List.of("ID", id),
                    List.of("Symbol", String.valueOf(symbol)),
                    List.of("Color", "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")"),
                    List.of("Walkable", String.valueOf(walkable))
            );
            return List.of(new DetailTable("", List.of("Field", "Value"), rows));
        }
    }
}

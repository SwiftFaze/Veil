package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileSandboxProviderTest {

    private static final String TILE_ID = "test:tile";

    @Test
    void exposesEveryTileAsAnEntryWithNamespaceAndCategory() {
        DevConsoleEntry grass = entryWithId("core:grass");

        assertEquals("core", grass.namespace());
        assertEquals("Tiles", grass.category());
    }

    @Test
    void localNameIsDerivedFromIdAfterColon() {
        assertEquals("water", entryWithId("core:water").name());
    }

    @Test
    void idWithoutColonIsItsOwnNamespaceAndLocalName() {
        TileSandboxProvider provider = new TileSandboxProvider(List.of(new FakeTile("bare")));

        DevConsoleEntry entry = provider.entries().get(0);

        assertEquals("bare", entry.namespace());
        assertEquals("bare", entry.name());
    }

    @Test
    void idStartingWithColonHasEmptyNamespace() {
        TileSandboxProvider provider = new TileSandboxProvider(List.of(new FakeTile(":leading")));

        DevConsoleEntry entry = provider.entries().get(0);

        assertEquals("", entry.namespace());
        assertEquals("leading", entry.name());
    }

    private static DevConsoleEntry entryWithId(String id) {
        return new TileSandboxProvider().entries().stream()
                .filter(entry -> id.equals(entry.id()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void createsPanelOpenedToTheRequestedTile() {
        TileSandboxProvider provider = new TileSandboxProvider();

        DetailsPaneWidget panel = (DetailsPaneWidget) provider.createPanel("core:grass");

        assertEquals(1, panel.getTableCount());
        TableWidget<List<String>> table = panel.getTable(0);
        table.moveToStart();
        List<String> idRow = table.getSelectedRow();
        assertEquals(List.of("ID", "core:grass"), idRow);
    }

    @Test
    void panelFocusesItsFirstTable() {
        TileSandboxProvider provider = new TileSandboxProvider(List.of(new FakeTile(TILE_ID)));

        DetailsPaneWidget panel = (DetailsPaneWidget) provider.createPanel(TILE_ID);

        assertTrue(panel.isTableFocused(0));
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
        TileSandboxProvider provider = new TileSandboxProvider(List.of(new FakeTile(TILE_ID)));

        DetailsPaneWidget panel = (DetailsPaneWidget) provider.createPanel(TILE_ID);

        assertEquals(1, panel.getTableCount());
        assertEquals(2, panel.getTable(0).getRowCount());
    }

    /**
     * Minimal Inspectable for testing.
     */
    private static class FakeTile implements Inspectable {
        private final String id;

        FakeTile(String id) {
            this.id = id;
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
            List<List<String>> rows = List.of(List.of("ID", id), List.of("Symbol", "x"));
            return List.of(new DetailTable("", List.of("Field", "Value"), rows));
        }
    }
}

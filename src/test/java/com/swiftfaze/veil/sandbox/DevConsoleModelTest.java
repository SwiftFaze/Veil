package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleModelTest {

    @Test
    void filtersEntriesBySubstringCaseInsensitively() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("MAG");

        List<String> names = model.filteredResults().stream().map(r -> r.entry().name()).toList();
        assertEquals(List.of("Mage"), names);
    }

    @Test
    void emptySearchTextIncludesAllEntries() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void noMatchesReturnsEmptyList() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage")));

        model.setSearchText("zzz");

        assertTrue(model.filteredResults().isEmpty());
    }

    @Test
    void matchesOnCategoryNotJustName() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("class");

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void matchesOnNamespaceNotJustName() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("core");

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void flattensEntriesAcrossMultipleProviders() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                stubProvider("Classes", "Mage"),
                stubProvider("Quests", "Goblin Slayer")
        ));

        assertEquals(2, model.filteredResults().size());
    }

    private DevConsoleProvider stubProvider(String category, String... names) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(names).stream()
                        .map(name -> new DevConsoleEntry("core", category, name))
                        .toList();
            }

            @Override
            public JComponent createPanel(String entryName) {
                return new JPanel();
            }
        };
    }
}

package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A single class's full computed stats, pre-selected to exactly the class a
 * dev-console search opened — no browsable list of every other class, unlike
 * {@link ClassSandboxPanel} (which stays as-is: it's a proof case the
 * pre-existing ui-component-framework.feature depends on, not part of the
 * dev console anymore).
 */
public class ClassDetailPanel extends JPanel {

    private static final List<Integer> SAMPLED_LEVELS = List.of(0, 5, 10, 15, 20);
    private final HeaderWidget header;
    private final TableWidget<List<String>> statsTable;

    public ClassDetailPanel(ClassSandboxModel model, String className) {
        this.header = new HeaderWidget(className);
        this.statsTable = TableWidget.ofRows(buildColumnHeaders(), detailRows(model, className));

        setBackground(WidgetTheme.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(header);
        add(statsTable);

        bindKeys();
    }

    public HeaderWidget getHeader() {
        return header;
    }

    public TableWidget<List<String>> getStatsTable() {
        return statsTable;
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);

        actionMap.put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statsTable.moveUp();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statsTable.moveDown();
            }
        });
    }

    private static List<String> buildColumnHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("Stat");
        for (int level : SAMPLED_LEVELS) {
            headers.add("Lv " + level);
        }
        return headers;
    }

    private static List<List<String>> detailRows(ClassSandboxModel model, String className) {
        return List.of(
                buildRow(model, className, "Attack Power", stats -> stats.getAttackPower()),
                buildRow(model, className, "Defense", stats -> stats.getDefense()),
                buildRow(model, className, "Max HP", stats -> stats.getMaxHp()),
                buildRow(model, className, "Max Mana", stats -> stats.getMaxMana()),
                buildRow(model, className, "Strength", stats -> stats.getStrength()),
                buildRow(model, className, "Dexterity", stats -> stats.getDexterity()),
                buildRow(model, className, "Constitution", stats -> stats.getConstitution()),
                buildRow(model, className, "Intelligence", stats -> stats.getIntelligence()),
                buildRow(model, className, "Wisdom", stats -> stats.getWisdom()),
                buildRow(model, className, "Luck", stats -> stats.getLuck())
        );
    }

    private static List<String> buildRow(ClassSandboxModel model, String className, String statName,
                                          java.util.function.Function<Stats, Integer> statGetter) {
        List<String> row = new ArrayList<>();
        row.add(statName);
        for (int level : SAMPLED_LEVELS) {
            Stats stats = model.computedStats(className, level);
            row.add(String.valueOf(statGetter.apply(stats)));
        }
        return row;
    }
}

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
import java.util.List;

/**
 * A single class's full computed stats, pre-selected to exactly the class a
 * dev-console search opened — no browsable list of every other class, unlike
 * {@link ClassSandboxPanel} (which stays as-is: it's a proof case the
 * pre-existing ui-component-framework.feature depends on, not part of the
 * dev console anymore).
 */
public class ClassDetailPanel extends JPanel {

    private final HeaderWidget header;
    private final TableWidget<List<String>> statsTable;

    public ClassDetailPanel(ClassSandboxModel model, String className) {
        this.header = new HeaderWidget(className);
        this.statsTable = TableWidget.ofRows(List.of("Field", "Value"), detailRows(model, className));

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

    private static List<List<String>> detailRows(ClassSandboxModel model, String className) {
        Stats stats = model.computedStats(className);
        return List.of(
                List.of("Attack Power", String.valueOf(stats.getAttackPower())),
                List.of("Defense", String.valueOf(stats.getDefense())),
                List.of("Max HP", String.valueOf(stats.getMaxHp())),
                List.of("Max Mana", String.valueOf(stats.getMaxMana())),
                List.of("Strength", String.valueOf(stats.getStrength())),
                List.of("Dexterity", String.valueOf(stats.getDexterity())),
                List.of("Constitution", String.valueOf(stats.getConstitution())),
                List.of("Intelligence", String.valueOf(stats.getIntelligence())),
                List.of("Wisdom", String.valueOf(stats.getWisdom())),
                List.of("Luck", String.valueOf(stats.getLuck()))
        );
    }
}

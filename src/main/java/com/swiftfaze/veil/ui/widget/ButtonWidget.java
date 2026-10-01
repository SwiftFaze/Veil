package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.input.Keybindings;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;

public class ButtonWidget extends Widget {

    private final JLabel label;
    private Runnable onConfirm = () -> {
    };

    public ButtonWidget(String text) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        label = new JLabel(text);
        label.setForeground(WidgetTheme.NORMAL_TEXT);
        label.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 16));
        label.setAlignmentX(LEFT_ALIGNMENT);
        add(label);

        bindKeys();
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_CONFIRM, "button-confirm");

        actionMap.put("button-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onConfirm.run();
            }
        });
    }
}

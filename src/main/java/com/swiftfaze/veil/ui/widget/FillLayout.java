package com.swiftfaze.veil.ui.widget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Stretches every child to the parent's full bounds, stacked on top of each
 * other — the layout half of a {@link javax.swing.JLayeredPane} overlay,
 * where z-order (not position) is what separates the layers.
 */
public class FillLayout implements LayoutManager {

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return parent.getPreferredSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof PopupWidget popup && !popup.isFullScreen()) {
                // Center non-full-screen popups at their preferred size
                Dimension prefSize = child.getPreferredSize();
                int x = (parent.getWidth() - prefSize.width) / 2;
                int y = (parent.getHeight() - prefSize.height) / 2;
                child.setBounds(x, y, prefSize.width, prefSize.height);
            } else {
                // Stretch full-screen and non-popup children to fill parent
                child.setBounds(0, 0, parent.getWidth(), parent.getHeight());
            }
        }
    }
}

package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;

/**
 * Live preview panel for the kitchen-sink scene with a marker, movement
 * controls, and a toggleable walkability overlay. Each time opened, the
 * panel starts fresh (marker at start position, overlay off).
 */
public class KitchenSinkPreviewPanel extends JPanel {
    private final KitchenSinkModel model;
    private final Camera camera;
    private final boolean hasAnyTiles;
    public static final String EMPTY_MESSAGE = "No tiles registered";

    public KitchenSinkPreviewPanel(WorldScene scene) {
        this.model = new KitchenSinkModel(scene);
        this.camera = new Camera(20, 15);
        this.hasAnyTiles = model.hasTiles();
        setFocusable(true);
        setBackground(WidgetTheme.BACKGROUND);
        if (!hasAnyTiles) {
            add(buildEmptyLabel());
        }
        bindKeys();
    }

    private void bindKeys() {
        var actionMap = getActionMap();
        var inputMap = getInputMap(WHEN_FOCUSED);

        inputMap.put(Keybindings.MOVE_UP_Z, Keybindings.ACTION_MOVE_UP);
        actionMap.put(Keybindings.ACTION_MOVE_UP, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                model.moveMarker(0, -1);
                repaint();
            }
        });

        inputMap.put(Keybindings.MOVE_UP_ARROW, Keybindings.ACTION_MOVE_UP);

        inputMap.put(Keybindings.MOVE_DOWN_S, Keybindings.ACTION_MOVE_DOWN);
        actionMap.put(Keybindings.ACTION_MOVE_DOWN, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                model.moveMarker(0, 1);
                repaint();
            }
        });

        inputMap.put(Keybindings.MOVE_DOWN_ARROW, Keybindings.ACTION_MOVE_DOWN);

        inputMap.put(Keybindings.MOVE_LEFT_Q, Keybindings.ACTION_MOVE_LEFT);
        actionMap.put(Keybindings.ACTION_MOVE_LEFT, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                model.moveMarker(-1, 0);
                repaint();
            }
        });

        inputMap.put(Keybindings.MOVE_LEFT_ARROW, Keybindings.ACTION_MOVE_LEFT);

        inputMap.put(Keybindings.MOVE_RIGHT_D, Keybindings.ACTION_MOVE_RIGHT);
        actionMap.put(Keybindings.ACTION_MOVE_RIGHT, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                model.moveMarker(1, 0);
                repaint();
            }
        });

        inputMap.put(Keybindings.MOVE_RIGHT_ARROW, Keybindings.ACTION_MOVE_RIGHT);

        inputMap.put(Keybindings.TOGGLE_WALKABILITY, Keybindings.ACTION_TOGGLE_WALKABILITY);
        actionMap.put(Keybindings.ACTION_TOGGLE_WALKABILITY, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                model.toggleOverlay();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (!hasAnyTiles) {
            return;
        }

        // Tint first so it sits behind the glyphs, leaving every glyph fully drawn.
        if (model.isOverlayOn()) {
            drawWalkabilityOverlay(g2d);
        }
        drawScene(g2d);
        drawMarker(g2d);
    }

    public boolean isPreviewShown() {
        return hasAnyTiles;
    }

    private static JLabel buildEmptyLabel() {
        JLabel label = new JLabel(EMPTY_MESSAGE);
        label.setForeground(WidgetTheme.NORMAL_TEXT);
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        return label;
    }

    private void drawScene(Graphics2D g2d) {
        model.getScene().renderWorld(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);
    }

    private void drawWalkabilityOverlay(Graphics2D g2d) {
        for (int x = 0; x < model.getScene().getWidth(); x++) {
            for (int y = 0; y < model.getScene().getHeight(); y++) {
                drawOverlayCell(g2d, x, y);
            }
        }
    }

    private void drawOverlayCell(Graphics2D g2d, int x, int y) {
        int tileW = GameConst.TILE_WIDTH;
        int tileH = GameConst.TILE_HEIGHT;
        int screenX = (x - camera.getX()) * tileW;
        int screenY = (y - camera.getY()) * tileH;

        boolean walkable = model.getScene().isWalkable(x, y);
        Color overlayColor = walkable ? WidgetTheme.VALID_HIGHLIGHT : WidgetTheme.INVALID_HIGHLIGHT;
        g2d.setColor(new Color(overlayColor.getRed(), overlayColor.getGreen(), overlayColor.getBlue(), 80));
        g2d.fillRect(screenX, screenY, tileW, tileH);
    }

    private void drawMarker(Graphics2D g2d) {
        int tileW = GameConst.TILE_WIDTH;
        int tileH = GameConst.TILE_HEIGHT;
        int screenX = (model.getMarkerX() - camera.getX()) * tileW;
        int screenY = (model.getMarkerY() - camera.getY()) * tileH + tileH;

        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        g2d.setColor(Color.decode("#ef481f"));
        g2d.drawString(String.valueOf('◼'), screenX, screenY);
    }

    public KitchenSinkModel getModel() {
        return model;
    }
}

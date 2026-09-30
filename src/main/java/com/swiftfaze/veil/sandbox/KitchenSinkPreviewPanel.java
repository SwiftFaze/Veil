package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.WorldScene;

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

    public KitchenSinkPreviewPanel(WorldScene scene) {
        this.model = new KitchenSinkModel(scene);
        this.camera = new Camera(20, 15);
        this.hasAnyTiles = scene.getWidth() > 0 && scene.getHeight() > 0;
        setFocusable(true);
        setBackground(WidgetTheme.BACKGROUND);
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
            drawEmptyMessage(g2d);
            return;
        }

        drawScene(g2d);
        if (model.isOverlayOn()) {
            drawWalkabilityOverlay(g2d);
        }
        drawMarker(g2d);
    }

    private void drawEmptyMessage(Graphics2D g2d) {
        g2d.setColor(WidgetTheme.NORMAL_TEXT);
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g2d.drawString("No tiles registered", 10, 30);
    }

    private void drawScene(Graphics2D g2d) {
        model.getScene().renderWorld(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);
    }

    private void drawWalkabilityOverlay(Graphics2D g2d) {
        int tileW = GameConst.TILE_WIDTH;
        int tileH = GameConst.TILE_HEIGHT;

        for (int x = 0; x < model.getScene().getWidth(); x++) {
            for (int y = 0; y < model.getScene().getHeight(); y++) {
                boolean walkable = model.getScene().isWalkable(x, y);
                drawOverlayCell(g2d, x, y, walkable, tileW, tileH);
            }
        }
    }

    private void drawOverlayCell(Graphics2D g2d, int x, int y, boolean walkable, int tileW, int tileH) {
        int screenX = (x - camera.getX()) * tileW;
        int screenY = (y - camera.getY()) * tileH;

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

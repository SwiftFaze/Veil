package com.swiftfaze.veil;

import java.awt.*;

public interface DrawableAsciiEntity extends Positionable {
    char getSymbol();
    Color getColor();
    void render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera);
}

package com.swiftfaze.veil;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AppIconTest {

    @Test
    void returnsNullWhenResourceIsAbsent() {
        Image icon = AppIcon.load(() -> null);
        assertNull(icon);
    }

    @Test
    void loadsImageWhenResourceExists() throws Exception {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", baos);
        byte[] imageData = baos.toByteArray();

        Image icon = AppIcon.load(() -> new java.io.ByteArrayInputStream(imageData));
        assertNotNull(icon);
    }

    @Test
    void returnsNullWhenImageReadFails() {
        // Pass invalid image data
        Image icon = AppIcon.load(() -> new java.io.ByteArrayInputStream("not an image".getBytes()));
        assertNull(icon);
    }
}

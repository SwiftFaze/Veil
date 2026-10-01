package com.swiftfaze.veil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Loads and manages the application icon from a classpath resource.
 * The icon is loaded from /icons/veil.png, a 512x512 RGBA PNG image.
 *
 * If the resource is absent or cannot be read, returns null and logs a warning.
 */
public class AppIcon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppIcon.class);
    private static final String RESOURCE_PATH = "/icons/veil.png";

    /**
     * Loads the icon from the classpath resource.
     * Returns null if the resource is absent or cannot be read.
     */
    public static Image load() {
        return load(() -> AppIcon.class.getResourceAsStream(RESOURCE_PATH));
    }

    /**
     * Loads the icon with a custom resource supplier, for testing.
     */
    public static Image load(Supplier<InputStream> resourceSupplier) {
        try (InputStream stream = resourceSupplier.get()) {
            if (stream == null) {
                LOGGER.warn("Icon resource not found at classpath {}", RESOURCE_PATH);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            LOGGER.warn("Failed to load icon resource", e);
            return null;
        }
    }
}

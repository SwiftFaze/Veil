package com.swiftfaze.veil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads and manages the application icon from a classpath resource.
 * The icon is loaded from /icons/veil.png, a 512x512 RGBA PNG image.
 *
 * If the resource is absent or cannot be read, logs a warning and does not
 * call the consumer.
 */
public class AppIcon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppIcon.class);
    private static final String RESOURCE_PATH = "/icons/veil.png";

    /**
     * Loads the icon from the classpath resource and applies it via the
     * provided consumer if successfully loaded.
     */
    public static void applyTo(Consumer<Image> consumer) {
        applyTo(consumer, () -> AppIcon.class.getResourceAsStream(RESOURCE_PATH));
    }

    /**
     * Loads the icon with a custom resource supplier and applies it via the
     * provided consumer if successfully loaded. For testing.
     */
    public static void applyTo(Consumer<Image> consumer, Supplier<InputStream> resourceSupplier) {
        Image icon = load(resourceSupplier);
        if (icon != null) {
            consumer.accept(icon);
        }
    }

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

package com.swiftfaze.veil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Loads and manages the application version from a filtered properties file.
 * The version is read from the classpath resource /version.properties, which
 * is populated at build time via Maven filtering with the project version.
 *
 * If the resource is absent, unfiltered (contains literal `${project.version}`),
 * or has no version key, the version string is empty and a warning is logged.
 */
public class AppVersion {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppVersion.class);
    private static final String RESOURCE_PATH = "/version.properties";
    private static final String VERSION_KEY = "version";

    private final String versionString;

    /**
     * Creates an instance that reads the classpath version resource.
     */
    public AppVersion() {
        this(() -> AppVersion.class.getResourceAsStream(RESOURCE_PATH));
    }

    /**
     * Creates an instance with a custom resource supplier, for testing.
     */
    public AppVersion(Supplier<InputStream> resourceSupplier) {
        this.versionString = readVersion(resourceSupplier);
    }

    /**
     * Returns the version display string, e.g. "v0.5.0-beta.39", or empty
     * if the version could not be loaded or is unfiltered.
     */
    public String getDisplayVersion() {
        return versionString;
    }

    private static String readVersion(Supplier<InputStream> resourceSupplier) {
        try (InputStream stream = resourceSupplier.get()) {
            if (stream == null) {
                LOGGER.warn("Version resource not found at classpath {}", RESOURCE_PATH);
                return "";
            }

            Properties props = new Properties();
            props.load(stream);

            String version = props.getProperty(VERSION_KEY, "").trim();
            if (version.isEmpty()) {
                LOGGER.warn("No version key found in {}", RESOURCE_PATH);
                return "";
            }

            // Check for unfiltered placeholder
            if (version.contains("${")) {
                LOGGER.warn("Version resource was not filtered; contains unprocessed placeholder: {}", version);
                return "";
            }

            return "v" + version;
        } catch (IOException e) {
            LOGGER.warn("Failed to read version resource", e);
            return "";
        }
    }
}

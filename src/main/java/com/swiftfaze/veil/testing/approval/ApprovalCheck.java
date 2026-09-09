package com.swiftfaze.veil.testing.approval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApprovalCheck {

    private static final Path APPROVED_DIR = Paths.get("src/test/resources/approved");

    public static void verify(String scenarioName, String actualGridAsText) {
        Path approvedPath = APPROVED_DIR.resolve(scenarioName + ".approved.txt");
        Path receivedPath = APPROVED_DIR.resolve(scenarioName + ".received.txt");

        try {
            String approved = readIfExists(approvedPath);
            String normalized = normalizeLineEndings(actualGridAsText);

            if (approved == null || !approved.equals(normalized)) {
                Files.createDirectories(APPROVED_DIR);
                Files.writeString(receivedPath, normalized, StandardCharsets.UTF_8);

                String approvedDisplay = approved != null ? approved : "(no approved fixture exists yet)";
                throw new AssertionError(
                    String.format(
                        "Approval test failed for '%s'.%n" +
                        "Approved:%n%s%n%n" +
                        "Received:%n%s",
                        scenarioName, approvedDisplay, normalized
                    )
                );
            }

            // On match, clean up any stale .received.txt
            if (Files.exists(receivedPath)) {
                Files.delete(receivedPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to verify approval: " + e.getMessage(), e);
        }
    }

    private static String readIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String normalizeLineEndings(String text) {
        return text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }
}

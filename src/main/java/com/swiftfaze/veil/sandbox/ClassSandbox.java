package com.swiftfaze.veil.sandbox;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Dev-only console entry point. Not referenced from Main.java and not the
 * packaged/jpackage build's entry point (see pom.xml's {@code main.class}) —
 * run it explicitly:
 * {@code mvn compile exec:java -Dexec.mainClass=com.swiftfaze.veil.sandbox.ClassSandbox}
 */
public class ClassSandbox {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClassSandbox::launch);
    }

    private static void launch() {
        List<DevConsoleProvider> providers = List.of(new ClassSandboxProvider());
        DevConsolePanel panel = new DevConsolePanel(new DevConsoleModel(providers));

        JFrame frame = new JFrame("Veil - Dev Console");
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.requestFocusInWindow();
    }
}

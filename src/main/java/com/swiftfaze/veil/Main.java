package com.swiftfaze.veil;

import com.swiftfaze.veil.config.SettingsConfig;
import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.mods.WidgetColorTheme;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.GameWindow;
import com.swiftfaze.veil.ui.HintAware;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PauseMenuPopup;
import com.swiftfaze.veil.ui.PauseToggleListener;
import com.swiftfaze.veil.ui.PopupToggleListener;
import com.swiftfaze.veil.ui.SettingsKeybindsPanel;
import com.swiftfaze.veil.ui.SettingsKeybindsWindow;
import com.swiftfaze.veil.ui.SettingsScreenPanel;
import com.swiftfaze.veil.ui.SettingsWindow;
import com.swiftfaze.veil.ui.TitleScreenPanel;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.FocusManager;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final List<ControlsHintBarWidget.Hint> GAME_HINTS = List.of(
            new ControlsHintBarWidget.Hint("i", "Inventory"),
            new ControlsHintBarWidget.Hint("x", "Codex"));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::loadGame);
    }

    private static void loadGame() {
        JFrame frame = new JFrame("Veil");
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        loadAndApplyDefaultTheme();
        Map<String, JComponent> cards = new HashMap<>();
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();

        GamePanel gamePanel = buildGameCard(cardPanel, cards, hintBar);
        buildUIScreens(cardLayout, cardPanel, cards, gamePanel, hintBar);
        wirePauseMenuNavigation(cards, cardLayout, cardPanel, gamePanel);
        configureAndShowFrame(frame, cardPanel, cardLayout, hintBar, cards);
    }

    private static GamePanel buildGameCard(JPanel cardPanel, Map<String, JComponent> cards, ControlsHintBarWidget hintBar) {
        GamePanel gamePanel = new GamePanel();
        InventoryPanel inventoryPanel = new InventoryPanel(hintBar);
        CodexPanel codexPanel = new CodexPanel(hintBar);
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        cards.put("pause", pauseMenuPopup);
        wirePopups(gamePanel, inventoryPanel, codexPanel, pauseMenuPopup, hintBar);

        JLayeredPane gameContentArea = GameWindow.buildContentArea(gamePanel);
        gameContentArea.add(inventoryPanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(codexPanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(pauseMenuPopup, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(inventoryPanel.getDropConfirmationPopup(), JLayeredPane.DRAG_LAYER);

        JPanel gameCardPanel = new JPanel(new BorderLayout());
        gameCardPanel.add(gameContentArea, BorderLayout.CENTER);
        cardPanel.add(gameCardPanel, "game");
        return gamePanel;
    }

    // Minimal stopgap wiring so the I/X toggles keep working with EastPanel gone — no
    // sidebar, no player-info display, just enough plumbing for the two popups to open/close/exclude
    // each other and hand focus back to the game on dismiss, same as EastPanel used to.
    private static void wirePopups(GamePanel gamePanel, InventoryPanel inventoryPanel, CodexPanel codexPanel,
                                    PauseMenuPopup pauseMenuPopup, ControlsHintBarWidget hintBar) {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        FocusManager focusManager = new FocusManager();

        inventoryPanel.setFocusManager(focusManager);
        inventoryPanel.showItems(mods.getAllItems());
        inventoryPanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));

        codexPanel.setFocusManager(focusManager);
        codexPanel.showItems(mods.getAllItems());
        codexPanel.showTiles(mods.getAllTiles());
        codexPanel.showClasses(mods.getAllPlayerClasses());
        codexPanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));

        pauseMenuPopup.setFocusManager(focusManager);
        pauseMenuPopup.setOnDismiss(() -> {
            gamePanel.setPaused(false);
            restoreGameFocus(gamePanel, hintBar);
        });

        gamePanel.addGameListener(new PopupToggleListener(inventoryPanel, codexPanel));
        gamePanel.addGameListener(new PauseToggleListener(gamePanel, pauseMenuPopup));
    }

    private static void restoreGameFocus(GamePanel gamePanel, ControlsHintBarWidget hintBar) {
        gamePanel.requestFocusInWindow();
        hintBar.setHints(GAME_HINTS);
    }

    private static void buildUIScreens(CardLayout cardLayout, JPanel cardPanel, Map<String, JComponent> cards,
                                        GamePanel gamePanel, ControlsHintBarWidget hintBar) {
        TitleScreenPanel titleScreen = new TitleScreenPanel(menuItem -> {
            handleMenuSelection(menuItem, cardLayout, cardPanel, cards, gamePanel);
            if ("New".equals(menuItem)) {
                hintBar.setHints(GAME_HINTS);
            }
        }, hintBar);
        SettingsStore settingsStore = new SettingsStore(Path.of("").toAbsolutePath());
        SettingsScreenPanel settingsScreen = new SettingsScreenPanel(
                screen -> handleSettingsBack(screen, cardLayout, cardPanel, cards), Main::openFolder, hintBar, settingsStore);
        SettingsKeybindsPanel keybindsScreen = new SettingsKeybindsPanel(
                screen -> navigateTo(cardLayout, cardPanel, cards, screen), hintBar, settingsStore);
        cards.put("title", titleScreen);
        cards.put("settings", settingsScreen);
        cards.put("keybinds", keybindsScreen);
        cardPanel.add(titleScreen, "title");
        cardPanel.add(SettingsWindow.buildContentArea(settingsScreen), "settings");
        cardPanel.add(SettingsKeybindsWindow.buildContentArea(keybindsScreen), "keybinds");
    }

    private static void handleMenuSelection(String menuItem, CardLayout cardLayout, JPanel cardPanel,
                                           Map<String, JComponent> cards, GamePanel gamePanel) {
        if ("New".equals(menuItem)) {
            cardLayout.show(cardPanel, "game");
            gamePanel.requestFocusInWindow();
            gamePanel.startGameLoop();
        } else if ("Settings".equals(menuItem)) {
            ((SettingsScreenPanel) cards.get("settings")).setBackTarget("title");
            navigateTo(cardLayout, cardPanel, cards, "settings");
        } else if ("Exit".equals(menuItem)) {
            System.exit(0);
        }
    }

    private static void handleSettingsBack(String screen, CardLayout cardLayout, JPanel cardPanel,
                                           Map<String, JComponent> cards) {
        if ("pause".equals(screen)) {
            cardLayout.show(cardPanel, "game");
            cards.get("pause").requestFocusInWindow();
        } else {
            navigateTo(cardLayout, cardPanel, cards, screen);
        }
    }

    private static void wirePauseMenuNavigation(Map<String, JComponent> cards, CardLayout cardLayout,
                                                 JPanel cardPanel, GamePanel gamePanel) {
        PauseMenuPopup pauseMenuPopup = (PauseMenuPopup) cards.get("pause");
        SettingsScreenPanel settingsScreen = (SettingsScreenPanel) cards.get("settings");
        pauseMenuPopup.setOnMenuSelect(item -> {
            if (PauseMenuPopup.SETTINGS.equals(item)) {
                settingsScreen.setBackTarget("pause");
                navigateTo(cardLayout, cardPanel, cards, "settings");
            } else if (PauseMenuPopup.EXIT_TO_MAIN_MENU.equals(item)) {
                gamePanel.resetState();
                navigateTo(cardLayout, cardPanel, cards, "title");
            }
        });
    }

    private static void configureAndShowFrame(JFrame frame, JPanel cardPanel, CardLayout cardLayout,
                                               ControlsHintBarWidget hintBar, Map<String, JComponent> cards) {
        // Border lives on the frame's content pane, not on any individual screen, so it's
        // flush against the true window edge and shows on every card (title/settings/
        // keybinds/game) uniformly rather than only around whichever panel drew its own.
        ((JComponent) frame.getContentPane()).setBorder(
                BorderFactory.createLineBorder(WidgetTheme.WINDOW_BORDER, 2));
        frame.setLayout(new BorderLayout());
        frame.add(cardPanel, BorderLayout.CENTER);
        frame.add(hintBar, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        wireWindowMode(frame, cards);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        cardLayout.show(cardPanel, "title");
        // cardPanel.getComponent(0) is whichever card was added to the container FIRST
        // (the "game" card, added in buildGameCard() before buildUIScreens() adds "title") -
        // not whichever card CardLayout is currently showing. Requesting focus on that
        // hidden, non-showing component silently fails, so no component ever holds
        // keyboard focus. Look the actually-visible card up by name instead.
        cards.get("title").requestFocusInWindow();
    }

    // Must run before any screen/widget is constructed below - they read WidgetTheme's
    // statics at construction time. No settings/config system exists yet to pick a
    // non-default theme, so this always applies whichever mod owns ID "core:default".
    private static void loadAndApplyDefaultTheme() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        WidgetColorTheme defaultTheme = mods.getTheme("core:default");
        if (defaultTheme != null) {
            WidgetTheme.applyTheme(defaultTheme);
        }
    }

    private static void navigateTo(CardLayout cardLayout, JPanel cardPanel,
                                    Map<String, JComponent> cards, String cardName) {
        cardLayout.show(cardPanel, cardName);
        JComponent target = cards.get(cardName);
        if (target != null) {
            target.requestFocusInWindow();
            if (target instanceof HintAware hintAware) {
                hintAware.refreshHints();
            }
        }
    }

    private static void openFolder(String which) {
        try {
            Path base = Path.of("").toAbsolutePath();
            File target = "mods".equals(which) ? base.resolve("mods").toFile() : base.toFile();
            if ("mods".equals(which) && !target.exists()) {
                Files.createDirectories(target.toPath());
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(target);
            }
        } catch (IOException e) {
            logger.warn("Failed to open folder: {}", which, e);
        }
    }

    private static void wireWindowMode(JFrame frame, Map<String, JComponent> cards) {
        SettingsScreenPanel settingsScreen = (SettingsScreenPanel) cards.get("settings");
        SettingsStore settingsStore = settingsScreen.getSettingsStore();
        settingsScreen.setOnWindowModeChanged(mode -> {
            applyWindowMode(frame, mode, settingsStore);
            settingsScreen.requestFocusInWindow();
        });
        registerWindowSizeShutdownHook(frame, settingsStore);
    }

    private static final int MIN_WINDOW_WIDTH = 400;
    private static final int MIN_WINDOW_HEIGHT = 300;

    private static void applyWindowMode(JFrame frame, String mode, SettingsStore settingsStore) {
        boolean fullscreen = "Fullscreen".equals(mode);
        boolean wasDisplayable = frame.isDisplayable();
        if (wasDisplayable) {
            frame.dispose(); // Swing requires this before setUndecorated on an already-shown frame
        }
        frame.setUndecorated(fullscreen);
        frame.setResizable(!fullscreen);
        if (fullscreen) {
            frame.setBounds(currentScreenBounds(frame));
        } else {
            frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
            frame.pack();
            restoreSavedWindowSize(frame, settingsStore);
        }
        if (wasDisplayable) {
            frame.setVisible(true);
        }
    }

    private static void restoreSavedWindowSize(JFrame frame, SettingsStore settingsStore) {
        SettingsConfig config = settingsStore.config();
        int savedWidth = config.getWindowWidth();
        int savedHeight = config.getWindowHeight();
        if (savedWidth > 0 && savedHeight > 0) {
            frame.setSize(clampToScreenBounds(frame, savedWidth, savedHeight));
        }
    }

    private static Dimension clampToScreenBounds(JFrame frame, int width, int height) {
        Rectangle bounds = currentScreenBounds(frame);
        int clampedWidth = Math.max(MIN_WINDOW_WIDTH, Math.min(width, bounds.width));
        int clampedHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(height, bounds.height));
        return new Dimension(clampedWidth, clampedHeight);
    }

    private static Rectangle currentScreenBounds(JFrame frame) {
        GraphicsConfiguration config = frame.getGraphicsConfiguration();
        GraphicsDevice device = config != null
                ? config.getDevice()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return device.getDefaultConfiguration().getBounds();
    }

    private static void registerWindowSizeShutdownHook(JFrame frame, SettingsStore settingsStore) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!frame.isUndecorated()) {
                SettingsConfig config = settingsStore.config();
                config.setWindowWidth(frame.getWidth());
                config.setWindowHeight(frame.getHeight());
                settingsStore.persist();
            }
        }));
    }

}

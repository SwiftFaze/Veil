package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassSandboxModelTest {

    @Test
    void listsAllKnownClasses() {
        ClassSandboxModel model = new ClassSandboxModel();

        List<String> names = model.classNames();

        assertTrue(names.contains("Warrior"));
        assertTrue(names.contains("Mage"));
    }

    @Test
    void computesWarriorStats() {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Warrior");

        assertEquals(35, stats.getAttackPower());
        assertEquals(17, stats.getDefense());
        assertEquals(120, stats.getMaxHp());
        assertEquals(20, stats.getMaxMana());
    }

    @Test
    void computesMageStats() {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Mage");

        assertEquals(16, stats.getAttackPower());
        assertEquals(11, stats.getDefense());
        assertEquals(70, stats.getMaxHp());
        assertEquals(100, stats.getMaxMana());
    }

    @Test
    void looksUpIdByClassName() {
        ClassSandboxModel model = new ClassSandboxModel();

        assertEquals("core:warrior", model.idFor("Warrior"));
        assertEquals("core:mage", model.idFor("Mage"));
    }
}

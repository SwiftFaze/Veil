package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.ClassDetailPanel;
import com.swiftfaze.veil.sandbox.ClassSandboxModel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ClassStatsLevelRangeSteps {

    private ClassSandboxModel model;
    private ClassDetailPanel detailPanel;
    private List<PlayerClass> classesForModel;
    private Exception lastException;

    @Given("a class {string} with base strength {int} growing by {string} and base max HP {int}")
    public void aClassWithBaseStrengthGrowingByAndBaseMaxHp(String className, int baseStrength, String growthCalc, int baseMaxHp) {
        // Create a fixture PlayerClass with the given properties
        Map<String, PlayerClass.StatCurve> statCurves = Map.of(
                "strength", new PlayerClass.StatCurve(baseStrength, growthCalc),
                "maxHp", new PlayerClass.StatCurve(baseMaxHp, null)
        );
        PlayerClass growerClass = new PlayerClass("test:grower", className, statCurves);
        classesForModel = List.of(growerClass);
        model = new ClassSandboxModel(classesForModel);
    }

    @Given("the core classes are loaded")
    public void theCoreClassesAreLoaded() {
        if (model == null || classesForModel == null || classesForModel.isEmpty()) {
            // Load the core classes from mods
            classesForModel = ModLoader.load(Paths.get("mods")).getAllPlayerClasses();
            model = new ClassSandboxModel(classesForModel);
        } else {
            // Append core classes to the existing list
            List<PlayerClass> allClasses = new ArrayList<>(classesForModel);
            allClasses.addAll(ModLoader.load(Paths.get("mods")).getAllPlayerClasses());
            model = new ClassSandboxModel(allClasses);
        }
    }

    @When("the detail view for {string} is opened")
    public void theDetailViewForIsOpened(String className) {
        try {
            lastException = null;
            detailPanel = new ClassDetailPanel(model, className);
        } catch (IllegalArgumentException e) {
            lastException = e;
        }
    }

    @Then("the detail table's columns are {string}")
    public void theDetailTableSColumnsAre(String expectedColumnsStr) {
        if (detailPanel == null) {
            fail("Detail panel is null; detail view may have failed to open");
        }
        List<String> expectedColumns = List.of(expectedColumnsStr.split(", "));
        List<String> actualColumns = detailPanel.getStatsTable().getColumnHeaders();
        assertEquals(expectedColumns, actualColumns,
                "Expected columns " + expectedColumns + " but got " + actualColumns);
    }

    @Then("the detail table's rows are the stats {string}")
    public void theDetailTableSRowsAreTheStats(String expectedRowsStr) {
        if (detailPanel == null) {
            fail("Detail panel is null; detail view may have failed to open");
        }
        List<String> expectedRows = List.of(expectedRowsStr.split(", "));
        int rowCount = detailPanel.getStatsTable().getRowCount();
        assertEquals(expectedRows.size(), rowCount,
                "Expected " + expectedRows.size() + " rows but got " + rowCount);

        for (int i = 0; i < expectedRows.size(); i++) {
            detailPanel.getStatsTable().moveToStart();
            for (int j = 0; j < i; j++) {
                detailPanel.getStatsTable().moveDown();
            }
            List<String> row = detailPanel.getStatsTable().getSelectedRow();
            assertEquals(expectedRows.get(i), row.get(0),
                    "Expected row " + i + " to be " + expectedRows.get(i) + " but got " + row.get(0));
        }
    }

    @Then("the {string} row reads {string}")
    public void theRowReads(String statName, String expectedValuesStr) {
        if (detailPanel == null) {
            fail("Detail panel is null; detail view may have failed to open");
        }
        // Find the row with the given stat name
        List<String> expectedValues = List.of(expectedValuesStr.split(", "));
        List<List<String>> allRows = new ArrayList<>();
        int rowCount = detailPanel.getStatsTable().getRowCount();

        for (int i = 0; i < rowCount; i++) {
            detailPanel.getStatsTable().moveToStart();
            for (int j = 0; j < i; j++) {
                detailPanel.getStatsTable().moveDown();
            }
            allRows.add(new ArrayList<>(detailPanel.getStatsTable().getSelectedRow()));
        }

        for (List<String> row : allRows) {
            if (!row.isEmpty() && row.get(0).equals(statName)) {
                // Found the row; check the values (skip the first element which is the stat name)
                List<String> actualValues = row.subList(1, row.size());
                assertEquals(expectedValues, actualValues,
                        "Expected " + statName + " row to have values " + expectedValues + " but got " + actualValues);
                return;
            }
        }
        fail("Row not found for stat: " + statName);
    }

    @Then("opening the detail view fails with {string}")
    public void openingTheDetailViewFailsWith(String expectedMessage) {
        if (lastException == null) {
            fail("Expected an exception with message '" + expectedMessage + "' but no exception was thrown");
        }
        assertEquals(expectedMessage, lastException.getMessage(),
                "Expected exception message '" + expectedMessage + "' but got '" + lastException.getMessage() + "'");
    }
}

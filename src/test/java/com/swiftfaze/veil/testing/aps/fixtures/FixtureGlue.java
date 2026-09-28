package com.swiftfaze.veil.testing.aps.fixtures;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step definitions for fixture features used by AcceptanceMutationSteps.
 * These are isolated in a separate package to avoid loading them in the main test suite.
 */
public class FixtureGlue {

    // Steps that ignore their integer argument (for testing survivors)
    @Given("the player gains {int} gold")
    public void playerGainsGold(int amount) {
        // Intentionally ignores the amount parameter
    }

    // Steps that check their integer argument (for testing mutant killing)
    @Given("the player gains {int} gold and it equals {int}")
    public void playerGainsGoldAndChecks(int amount, int expected) {
        assertEquals(expected, amount);
    }

    // Steps that work with strings (for testing string mutations)
    @Given("the player chooses {string}")
    public void playerChooses(String className) {
        // Stores the choice (in a real scenario)
    }

    @When("they change their mind to {string}")
    public void changeChoice(String newClass) {
        // Verifies the change (in a real scenario)
    }

    // Steps for Examples rows
    @Given("a damage value of {int}")
    public void damageValue(int x) {
        // Stores x
    }

    @When("combined with {int}")
    public void combineWith(int y) {
        // Combines with y
    }

    @Then("the total is checked")
    public void totalIsChecked() {
        // Verifies the combination (in a real scenario)
    }

    // Steps for multiple scenarios
    @Given("something happens with {int}")
    public void somethingHappens(int value) {
        // Handles first scenario
    }

    @Given("something different happens with {int}")
    public void somethingDifferent(int value) {
        // Handles second scenario
    }

    // Steps for tagged features (these won't be used if features are skipped)
    @Given("something that needs manual testing")
    public void manualTestingStep() {
        // For @manual-verification feature
    }

    @Given("something undefined")
    public void undefinedStep() {
        // For @pending feature (this step won't be needed if feature is skipped)
    }

    // Step that always fails (for testing failure detection)
    @Given("a condition that fails")
    public void conditionThatFails() {
        fail("This condition intentionally fails");
    }
}

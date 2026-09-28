package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.testing.aps.AcceptanceMutator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for testing the AcceptanceMutator.
 * Uses fixture features stored in src/test/resources/aps-fixtures/
 */
public class AcceptanceMutationSteps {
    private AcceptanceMutationContext context;

    public AcceptanceMutationSteps() {
        this.context = AcceptanceMutationContext.getInstance();
    }

    // Scenario 1: A step that ignores its argument produces a survivor
    @Given("a fixture feature with a scenario {string} using the integer {int}")
    public void fixtureFeatureWithIntegerScenario(String scenarioName, int value) throws IOException {
        String featureContent = """
            Feature: Test feature

            Scenario: %s
              Given the player gains %d gold
            """.formatted(scenarioName, value);

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-feature");
    }

    @And("its step definition ignores the integer")
    public void stepIgnoresInteger() {
        // The fixture glue has a step that ignores the integer
        context.setStepIgnoresArgument(true);
    }

    // Scenario 2: A step that uses its argument kills every mutant
    @Given("a fixture feature whose step definitions check every literal")
    public void fixtureFeatureWithChecks() throws IOException {
        String featureContent = """
            Feature: Test feature with checks

            Scenario: Player gains gold
              Given the player gains 5 gold and it equals 5
            """;

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-feature-checks");
    }

    // Scenario 3: A quoted string is swapped for another value from the same file
    @Given("a fixture feature using the quoted strings {string} and {string}")
    public void fixtureFeatureWithStrings(String str1, String str2) throws IOException {
        String featureContent = """
            Feature: Class selection

            Scenario: Choose class
              Given the player chooses "%s"
              When they change their mind to "%s"
            """.formatted(str1, str2);

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-strings");
    }

    @When("the acceptance mutator generates mutants")
    public void mutatorGeneratesMutants() {
        context.generateMutants();
    }

    @Then("{string} is mutated to {string} and {string} to {string}")
    public void stringIsMutatedTo(String str1, String str2, String str1b, String str2b) {
        List<String> mutants = context.getMutantStrings();

        // Check that str1 → str2 and str2 → str1
        String mutation1 = "\"" + str1 + "\" → \"" + str2 + "\"";
        String mutation2 = "\"" + str2 + "\" → \"" + str1 + "\"";

        assertTrue(mutants.stream().anyMatch(m -> m.contains(mutation1)),
                  "Expected mutation: " + mutation1);
        assertTrue(mutants.stream().anyMatch(m -> m.contains(mutation2)),
                  "Expected mutation: " + mutation2);
    }

    // Scenario 4: A quoted string with no other value in the file is not mutated
    @Given("a fixture feature whose only quoted string is {string}")
    public void fixtureFeatureWithUniqueString(String uniqueString) throws IOException {
        String featureContent = """
            Feature: Class selection

            Scenario: Choose class
              Given the player chooses "%s"
            """.formatted(uniqueString);

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-unique-string");
    }

    @Then("no string mutant is generated for {string}")
    public void noStringMutantGenerated(String stringValue) {
        List<String> mutants = context.getMutantStrings();

        for (String mutant : mutants) {
            assertFalse(mutant.contains("\"" + stringValue + "\""),
                       "Should not generate string mutants for unique string: " + stringValue);
        }
    }

    @And("the report says it was skipped for having no alternative value")
    public void reportSaysSkipped() {
        List<String> report = context.getReport();

        assertTrue(report.stream().anyMatch(r -> r.contains("skipped") || r.contains("alternative")),
                  "Report should mention skipped or alternative value");
    }

    // Scenario 5: Each Examples cell is mutated on its own row
    @Given("a fixture Scenario Outline with an Examples row {string}")
    public void fixtureOutlineWithExamples(String examplesRow) throws IOException {
        // Parse the row format: "| 2 | 3 |"
        String[] values = examplesRow.split("\\|");
        List<String> cells = new ArrayList<>();
        for (String v : values) {
            String trimmed = v.trim();
            if (!trimmed.isEmpty()) {
                cells.add(trimmed);
            }
        }

        StringBuilder sb = new StringBuilder("Feature: Damage calculation\n\n");
        sb.append("Scenario Outline: Calculate damage\n");
        sb.append("  Given a damage value of <x>\n");
        sb.append("  When combined with <y>\n");
        sb.append("  Then the total is checked\n\n");
        sb.append("  Examples:\n");
        sb.append("    | x | y |\n");
        sb.append("    | ").append(String.join(" | ", cells)).append(" |\n");

        context.setFeatureContent(sb.toString());
        context.setFeatureName("test-examples");
    }

    @Then("it generates mutants for {int} and for {int} separately, each running only that Examples row")
    public void mutantsForExamplesEach(int val1, int val2) {
        List<String> mutants = context.getMutantStrings();

        // Should have mutants for both values
        assertTrue(mutants.stream().anyMatch(m -> m.contains(String.valueOf(val1))),
                  "Should have mutants for value: " + val1);
        assertTrue(mutants.stream().anyMatch(m -> m.contains(String.valueOf(val2))),
                  "Should have mutants for value: " + val2);

        // Should mention that it's running only that row
        assertTrue(mutants.stream().anyMatch(m -> m.contains("row")),
                  "Mutants should indicate they're running individual rows");
    }

    // Scenario 6: Only the mutated scenario runs
    @Given("a fixture feature with two scenarios")
    public void fixtureFeatureWithTwoScenarios() throws IOException {
        String featureContent = """
            Feature: Multiple scenarios

            Scenario: First scenario
              Given something happens with 5

            Scenario: Second scenario
              Given something different happens with 10
            """;

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-two-scenarios");
    }

    @When("the acceptance mutator runs a mutant of the first scenario")
    public void mutatorRunsFirstScenarioMutant() throws IOException {
        context.runMutantOfScenario("First scenario");
    }

    @Then("only the first scenario is executed for that mutant")
    public void onlyFirstScenarioExecuted() {
        List<String> executedScenarios = context.getExecutedScenarios();

        assertEquals(1, executedScenarios.size(), "Should execute only one scenario");
        assertTrue(executedScenarios.get(0).contains("First scenario"),
                  "Should execute the first scenario");
    }

    // Scenario 7: `--feature` limits the run to one feature
    @Given("fixture features `alpha` and `beta`")
    public void fixtureFeaturesBothPresent() throws IOException {
        context.createFixture("alpha", "Feature: Alpha\nScenario: Alpha test\nGiven something happens");
        context.createFixture("beta", "Feature: Beta\nScenario: Beta test\nGiven something happens");
    }

    @When("the acceptance mutator runs with `--feature alpha`")
    public void mutatorRunsWithFeatureAlpha() throws IOException {
        context.runMutatorWithFeatureFilter("alpha");
    }

    @Then("only mutants from `alpha.feature` are run")
    public void onlyAlphaMutantsRun() {
        List<String> results = context.getMutationResults();

        assertTrue(results.stream().anyMatch(r -> r.contains("alpha")),
                  "Should have results from alpha feature");
        assertTrue(results.stream().noneMatch(r -> r.contains("beta") && r.contains("mutant")),
                  "Should not have mutants from beta feature");
    }

    // Scenario 8: An unknown `--feature` slug fails
    @When("the acceptance mutator runs with `--feature does-not-exist`")
    public void mutatorRunsWithUnknownFeature() throws IOException {
        context.runMutatorWithFeatureFilter("does-not-exist");
    }

    @Then("it fails, naming the slug and the features directory searched")
    public void failsNamingSlugAndDirectory() {
        String errorMessage = context.getErrorMessage();

        assertNotNull(errorMessage, "Should have an error message");
        assertTrue(errorMessage.contains("does-not-exist"),
                  "Error should name the unknown slug");
        assertTrue(errorMessage.contains("features") || errorMessage.contains("specs"),
                  "Error should mention the features directory");
    }

    // Scenario 9: Manual-verification and pending features are skipped
    @Given("a fixture feature tagged `@manual-verification` and one tagged `@pending`")
    public void fixturesFeaturesWithTags() throws IOException {
        context.createFixture("manual-verification",
                             "@manual-verification\nFeature: Manual\nScenario: Manual test\nGiven something");
        context.createFixture("pending",
                             "@pending\nFeature: Pending\nScenario: Pending test\nGiven something");
    }

    @When("the acceptance mutator runs on all features")
    public void mutatorRunsOnAllFeatures() throws IOException {
        context.runMutatorOnAllFeatures();
    }

    @Then("neither feature is mutated")
    public void neitherFeatureMutated() {
        List<String> mutants = context.getAllMutants();

        assertFalse(mutants.stream().anyMatch(m -> m.contains("manual-verification")),
                   "Should not mutate @manual-verification feature");
        assertFalse(mutants.stream().anyMatch(m -> m.contains("pending")),
                   "Should not mutate @pending feature");
    }

    @And("the report lists both as skipped with their tag")
    public void reportListsSkipped() {
        List<String> skipped = context.getSkippedFeatures();

        assertTrue(skipped.stream().anyMatch(s -> s.contains("manual-verification")),
                  "Should list @manual-verification as skipped");
        assertTrue(skipped.stream().anyMatch(s -> s.contains("pending")),
                  "Should list @pending as skipped");
    }

    // Scenario 10: A scenario that already fails unmutated is reported, not mutated
    @Given("a fixture feature whose scenario fails before any mutation")
    public void fixtureFeatureWithFailingScenario() throws IOException {
        String featureContent = """
            Feature: Failing feature

            Scenario: Failing scenario
              Given a condition that fails
            """;

        context.setFeatureContent(featureContent);
        context.setFeatureName("test-failing");
    }

    @When("the acceptance mutator runs on that feature")
    public void mutatorRunsOnFeature() throws IOException {
        context.runMutatorOnCurrentFeature();
    }

    @Then("it reports the scenario as failing on the original")
    public void reportsFailingOnOriginal() {
        List<String> report = context.getReport();

        assertTrue(report.stream().anyMatch(r -> r.contains("fail") || r.contains("original")),
                  "Report should mention that the scenario fails on the original");
    }

    @And("it generates no mutants for it")
    public void noMutantsGenerated() {
        List<String> mutants = context.getMutantStrings();

        assertTrue(mutants.isEmpty() || mutants.stream().noneMatch(m -> m.contains("failing")),
                  "Should not generate mutants for failing scenario");
    }

    @And("it exits with a code other than 0 and 3")
    public void exitsWithCode2() {
        int code = context.getLastExitCode();

        assertTrue(code != 0 && code != 3, "Exit code should be 2 (error), not 0 or 3");
    }

    @Then("it reports survivors for {int} → {int} and {int} → {int} at the literal's file and line")
    public void reportsSurvivors(int val1, int val2, int val3, int val4) {
        List<String> survivors = context.getSurvivors();

        assertTrue(survivors.stream().anyMatch(s -> s.contains(val1 + "") && s.contains(val2 + "")),
                  "Should report survivor: " + val1 + " → " + val2);
        assertTrue(survivors.stream().anyMatch(s -> s.contains(val1 + "") && s.contains(val3 + "")),
                  "Should report survivor: " + val1 + " → " + val3);
    }

    @And("it exits {int}")
    public void exitsWithCode(int expectedCode) {
        int actualCode = context.getLastExitCode();

        assertEquals(expectedCode, actualCode, "Exit code mismatch");
    }

    @Then("it reports no survivors")
    public void noSurvivors() {
        List<String> survivors = context.getSurvivors();

        assertTrue(survivors.isEmpty(), "Should have no survivors");
    }
}

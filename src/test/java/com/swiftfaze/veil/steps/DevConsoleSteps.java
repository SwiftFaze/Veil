package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.sandbox.ClassSandboxProvider;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DevConsoleSteps {

    private DevConsoleModel model;
    private DevConsolePanel panel;

    @Given("the dev console is running with the {string} provider registered")
    public void theDevConsoleIsRunningWithTheProviderRegistered(String providerName) {
        List<DevConsoleProvider> providers = List.of(providerFor(providerName));
        model = new DevConsoleModel(providers);
        panel = new DevConsolePanel(model);
    }

    @When("the search text is set to {string}")
    public void theSearchTextIsSetTo(String text) {
        panel.getSearchField().setText(text);
    }

    @Then("the results include an entry named {string}")
    public void theResultsIncludeAnEntryNamed(String name) {
        assertTrue(resultNames().contains(name));
    }

    @Then("the results do not include an entry named {string}")
    public void theResultsDoNotIncludeAnEntryNamed(String name) {
        assertFalse(resultNames().contains(name));
    }

    @Then("the results are empty")
    public void theResultsAreEmpty() {
        assertTrue(model.filteredResults().isEmpty());
    }

    @Then("the {string} result has namespace {string} and category {string}")
    public void theResultHasNamespaceAndCategory(String name, String namespace, String category) {
        DevConsoleModel.SearchResult result = findResult(name);
        assertEquals(namespace, result.entry().namespace());
        assertEquals(category, result.entry().category());
    }

    @When("{string} is opened")
    public void isOpened(String entryName) {
        panel.getSearchField().setText(entryName);
        panel.confirmSelection();
    }

    @Then("the opened detail panel is shown")
    public void theOpenedDetailPanelIsShown() {
        assertTrue(panel.isProviderPanelShowing());
    }

    @When("the back action is triggered")
    public void theBackActionIsTriggered() {
        panel.showSearchView();
    }

    @When("opening the selection does nothing")
    public void openingTheSelectionDoesNothing() {
        panel.confirmSelection();
        assertFalse(panel.isProviderPanelShowing());
    }

    private List<String> resultNames() {
        return model.filteredResults().stream().map(result -> result.entry().name()).toList();
    }

    private DevConsoleModel.SearchResult findResult(String name) {
        return model.filteredResults().stream()
                .filter(result -> result.entry().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No result named: " + name));
    }

    private DevConsoleProvider providerFor(String name) {
        if ("Classes".equals(name)) {
            return new ClassSandboxProvider();
        }
        throw new IllegalArgumentException("Unknown provider: " + name);
    }
}

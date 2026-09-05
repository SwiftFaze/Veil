package com.swiftfaze.veil.sandbox;

import java.util.List;
import java.util.Locale;

public class DevConsoleModel {

    private final List<SearchResult> allResults;
    private String searchText = "";

    public DevConsoleModel(List<DevConsoleProvider> providers) {
        this.allResults = providers.stream()
                .flatMap(provider -> provider.entries().stream().map(entry -> new SearchResult(provider, entry)))
                .toList();
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    public List<SearchResult> filteredResults() {
        String needle = searchText.toLowerCase(Locale.ROOT);
        return allResults.stream()
                .filter(result -> matches(result.entry(), needle))
                .toList();
    }

    private boolean matches(DevConsoleEntry entry, String needle) {
        return contains(entry.namespace(), needle)
                || contains(entry.category(), needle)
                || contains(entry.name(), needle);
    }

    private boolean contains(String field, String needle) {
        return field.toLowerCase(Locale.ROOT).contains(needle);
    }

    /**
     * Pairs a matched entry back with the provider that owns it, so the panel can open its
     * detail view without every entry having to carry its own opener.
     */
    public record SearchResult(DevConsoleProvider provider, DevConsoleEntry entry) {
    }
}

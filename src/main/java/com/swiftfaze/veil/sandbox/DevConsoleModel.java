package com.swiftfaze.veil.sandbox;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    public Optional<SearchResult> findById(String id) {
        return allResults.stream()
                .filter(result -> result.entry().id().equals(id))
                .findFirst();
    }

    /**
     * Resolves an entry by either its full id ("core:player") or just the part after the last
     * ":" ("player") - the dev console's set/add/subtract verbs address entries by the shorter
     * form since #170's examples never use the namespace prefix, unlike `edit`.
     */
    public Optional<SearchResult> findByEntryToken(String token) {
        return allResults.stream()
                .filter(result -> matchesToken(result.entry(), token))
                .findFirst();
    }

    public List<SearchResult> allResults() {
        return allResults;
    }

    private boolean matchesToken(DevConsoleEntry entry, String token) {
        return entry.id().equals(token) || localId(entry.id()).equals(token);
    }

    private String localId(String id) {
        int colonIndex = id.indexOf(':');
        return colonIndex < 0 ? id : id.substring(colonIndex + 1);
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

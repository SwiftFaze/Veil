package com.swiftfaze.veil.sandbox;

/**
 * Lets a {@link DevConsoleProvider} wire live field mutation into the command bar's set/add/
 * subtract verbs. Only {@link PlayerSandboxProvider} implements one for v1 - a provider with no
 * mutable state (e.g. {@code ClassSandboxProvider}) simply doesn't return one from
 * {@link DevConsoleProvider#fieldMutator(String)}.
 */
public interface DevConsoleFieldMutator {

    /**
     * @param fieldToken the field's abbreviation token (e.g. "str", "maxhp")
     * @param rawValue   the typed value token - a base-10 integer, or "default" (SET only, and
     *                   only for fields with a class-default to reset to)
     */
    DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue);
}

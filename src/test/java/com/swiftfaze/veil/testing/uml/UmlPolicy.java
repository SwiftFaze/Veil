package com.swiftfaze.veil.testing.uml;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the {@code :levels} vector of {@code docs/uml/veil.policy.edn}: groups of
 * top-level package segments, inner (index 0) to outer. Only that one key is read,
 * so no EDN library is needed.
 */
final class UmlPolicy {

    private static final String LEVELS_KEY = ":levels";

    private UmlPolicy() {
    }

    static List<List<String>> levels(String policyEdn) {
        int key = policyEdn.indexOf(LEVELS_KEY);
        if (key < 0) {
            throw new IllegalArgumentException("policy has no :levels");
        }
        List<List<String>> groups = new ArrayList<>();
        List<String> group = null;
        StringBuilder token = new StringBuilder();
        int depth = 0;
        for (int i = policyEdn.indexOf('[', key); i < policyEdn.length(); i++) {
            char c = policyEdn.charAt(i);
            if (c == '[') {
                depth++;
                if (depth == 2) {
                    group = new ArrayList<>();
                }
            } else if (c == ']' || Character.isWhitespace(c) || c == ',') {
                flush(group, token);
                if (c == ']') {
                    depth--;
                    if (depth == 1) {
                        groups.add(group);
                    } else if (depth == 0) {
                        return groups;
                    }
                }
            } else {
                token.append(c);
            }
        }
        throw new IllegalArgumentException("unbalanced :levels vector");
    }

    private static void flush(List<String> group, StringBuilder token) {
        if (token.length() > 0 && group != null) {
            group.add(token.toString());
        }
        token.setLength(0);
    }
}

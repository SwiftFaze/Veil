package com.swiftfaze.veil.testing.qa;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * In-order subsequence match: every expected event must appear in the recorded
 * events, in order, with any number of other events allowed in between.
 */
public final class EventMatcher {
    private EventMatcher() {
    }

    /**
     * Outcome of a match.
     *
     * @param passed         whether every expected event was matched
     * @param expected       the expected events
     * @param matchedCount   how many expected events were matched, in order
     * @param recordedAfter  the recorded events after the last match (all of them if none matched)
     */
    public record Result(boolean passed, int matchedCount, List<JsonObject> expected, List<JsonObject> recordedAfter) {

        public String describeFailure() {
            if (passed) {
                return "";
            }
            StringBuilder out = new StringBuilder();
            out.append("first unmatched expected event (#").append(matchedCount + 1).append(" of ")
                    .append(expected.size()).append("): ").append(expected.get(matchedCount)).append('\n');
            out.append("events recorded after the last match (")
                    .append(matchedCount == 0 ? "none matched" : "expected #" + matchedCount).append("):");
            if (recordedAfter.isEmpty()) {
                out.append("\n  (none)");
            }
            recordedAfter.forEach(event -> out.append("\n  ").append(event));
            return out.toString();
        }
    }

    public static Result match(List<JsonObject> expected, List<JsonObject> recorded) {
        int matched = 0;
        int lastMatchIndex = -1;
        for (int i = 0; i < recorded.size() && matched < expected.size(); i++) {
            if (expected.get(matched).equals(recorded.get(i))) {
                matched++;
                lastMatchIndex = i;
            }
        }
        List<JsonObject> after = List.copyOf(recorded.subList(lastMatchIndex + 1, recorded.size()));
        return new Result(matched == expected.size(), matched, List.copyOf(expected), after);
    }
}

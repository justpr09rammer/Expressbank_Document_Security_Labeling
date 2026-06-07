package org.example.service;

import org.example.model.LabelRule;
import org.example.model.SecurityLabel;

import java.util.*;
import java.util.regex.*;


public class RuleBasedClassifier {

    private static final int CONTEXT_CHARS = 60;

    public record MatchDetail(
            String matchedText,
            int    startIndex,
            int    endIndex,
            String snippet
    ) {}


    public record PatternTestResult(
            String         pattern,
            boolean        valid,
            boolean        matched,
            List<MatchDetail> matches,
            String         errorMessage
    ) {
        public boolean isMatch() { return valid && matched; }
    }


    public record ClassificationResult(
            SecurityLabel          assignedLabel,
            double                 confidenceScore,
            List<RuleMatchResult>  ruleMatches,
            int                    totalRulesChecked
    ) {}

    public record RuleMatchResult(
            LabelRule         rule,
            List<MatchDetail> matches
    ) {}

    // ─────────────────────────────────────────────────────────────────────
    // 1. Raw pattern tester
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Test an arbitrary regex pattern against the given text.
     * Returns all matches (up to {@code maxMatches}).
     *
     * @param pattern   regex string (supports (?i) inline flag or will use CASE_INSENSITIVE by default)
     * @param text      text to search
     * @param maxMatches cap on returned matches (0 = unlimited)
     */
    public PatternTestResult testPattern(String pattern, String text, int maxMatches) {
        if (pattern == null || pattern.isBlank()) {
            return new PatternTestResult(pattern, false, false,
                    List.of(), "Pattern is empty");
        }
        if (text == null) text = "";

        Pattern compiled;
        try {
            // Honour inline flags like (?i); add CASE_INSENSITIVE as default only if absent
            int flags = pattern.startsWith("(?") ? 0
                    : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            compiled = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            return new PatternTestResult(pattern, false, false,
                    List.of(), "Invalid regex: " + e.getMessage());
        }

        Matcher m = compiled.matcher(text);
        List<MatchDetail> matches = new ArrayList<>();
        int limit = (maxMatches <= 0) ? Integer.MAX_VALUE : maxMatches;

        while (m.find() && matches.size() < limit) {
            matches.add(buildDetail(text, m.start(), m.end()));
        }

        return new PatternTestResult(pattern, true, !matches.isEmpty(), matches, null);
    }

    public PatternTestResult testPattern(String pattern, String text) {
        return testPattern(pattern, text, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Single rule tester
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Test a specific {@link LabelRule} against a string.
     * Returns a PatternTestResult tagged with the rule's pattern.
     */
    public PatternTestResult testRule(LabelRule rule, String text) {
        if (rule == null) {
            return new PatternTestResult(null, false, false,
                    List.of(), "Rule is null");
        }
        return testPattern(rule.getPattern(), text, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Full classification — all rules vs. document text
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Run all enabled rules against {@code text} and return a {@link ClassificationResult}.
     * Label selection: highest cumulative weight wins
     *   weight = sum of (rule.priority × matchCount) for each label
     *
     * @param rules list of rules (only enabled ones are evaluated)
     * @param text  document text
     */
    public ClassificationResult classify(List<LabelRule> rules, String text) {
        if (text == null) text = "";

        List<LabelRule> enabled = rules.stream()
                .filter(LabelRule::isEnabled)
                .sorted(Comparator.comparingInt(LabelRule::getPriority).reversed())
                .toList();

        List<RuleMatchResult>        ruleMatches  = new ArrayList<>();
        Map<SecurityLabel, Integer>  weightMap    = new EnumMap<>(SecurityLabel.class);

        for (LabelRule rule : enabled) {
            PatternTestResult result = testPattern(rule.getPattern(), text, 0);
            if (result.isMatch()) {
                ruleMatches.add(new RuleMatchResult(rule, result.matches()));
                int weight = rule.getPriority() * result.matches().size();
                weightMap.merge(rule.getLabel(), weight, Integer::sum);
            }
        }

        // Pick label with highest accumulated weight
        SecurityLabel assigned = weightMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(text.isBlank() ? SecurityLabel.PUBLIC : SecurityLabel.INTERNAL_ONLY);

        double confidence = computeConfidence(weightMap, assigned, enabled.size());

        return new ClassificationResult(assigned, confidence, ruleMatches, enabled.size());
    }
    private MatchDetail buildDetail(String text, int start, int end) {
        String matched = text.substring(start, end);
        int ctxStart   = Math.max(0, start - CONTEXT_CHARS);
        int ctxEnd     = Math.min(text.length(), end + CONTEXT_CHARS);
        String snippet = "…"
                + text.substring(ctxStart, start)
                + "【" + matched + "】"          // markers so callers can highlight
                + text.substring(end, ctxEnd)
                + "…";
        return new MatchDetail(matched, start, end, snippet.replace("\n", " ").trim());
    }

    private double computeConfidence(Map<SecurityLabel, Integer> weightMap,
                                     SecurityLabel assigned,
                                     int totalRules) {
        if (totalRules == 0 || assigned == null) return 0.5;
        int total   = weightMap.values().stream().mapToInt(Integer::intValue).sum();
        if (total   == 0) return 0.4;
        double base = (double) weightMap.getOrDefault(assigned, 0) / total;
        double boost = assigned.getSeverity() * 0.03;
        return Math.min(0.99, base + boost);
    }
}
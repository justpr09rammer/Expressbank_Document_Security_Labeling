package org.example.service;

import org.example.model.LabelResult;
import org.example.model.LabelRule;
import org.example.model.SecurityLabel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core labeling service.
 * Supports both manual and automatic (regex-based) labeling.
 */
public class LabelingService {

    private final DocumentParser parser;
    private final RulesManager rulesManager;
    private final RuleBasedClassifier classifier;

    public LabelingService(DocumentParser parser, RulesManager rulesManager) {
        this.parser = parser;
        this.rulesManager = rulesManager;
        this.classifier = new RuleBasedClassifier();
    }

    // ── Manual Labeling ───────────────────────────────────────────────────

    /**
     * Apply a user-selected label without content analysis.
     */
    public LabelResult applyManualLabel(String filePath,
                                        SecurityLabel chosenLabel) {

        String fileName = extractFileName(filePath);

        LabelResult result =
                new LabelResult(
                        filePath,
                        fileName,
                        chosenLabel,
                        LabelResult.Mode.MANUAL);

        result.setConfidenceScore(1.0);

        try {
            DocumentParser.ParseResult parsed = parser.parse(filePath);

            result.setWordCount(parsed.wordCount());
            result.setFileSizeBytes(parsed.fileSizeBytes());
            result.setFileType(parsed.fileType());

        } catch (IOException | UnsupportedOperationException e) {
            // Metadata unavailable, label is still valid
            result.setWordCount(0);
        }

        return result;
    }

    // ── Automatic Labeling ────────────────────────────────────────────────

    /**
     * Analyse document text using enabled rules and assign
     * the highest-confidence security label.
     */
    public LabelResult applyAutoLabel(String filePath) throws IOException {

        String fileName = extractFileName(filePath);

        DocumentParser.ParseResult parsed = parser.parse(filePath);
        String text = parsed.text();

        LabelResult result =
                new LabelResult(
                        filePath,
                        fileName,
                        null,
                        LabelResult.Mode.AUTO);

        result.setWordCount(parsed.wordCount());
        result.setFileSizeBytes(parsed.fileSizeBytes());
        result.setFileType(parsed.fileType());

        rulesManager.sortByPriority();
        List<LabelRule> enabledRules = rulesManager.getEnabledRules();

        RuleBasedClassifier.ClassificationResult classification =
                classifier.classify(enabledRules, text);

        populateMatchDetails(result, classification);

        result.setLabel(classification.assignedLabel());
        result.setConfidenceScore(classification.confidenceScore());

        return result;
    }

    // ── Regex Testing Utilities ───────────────────────────────────────────

    /**
     * Test a raw regex pattern against text.
     */
    public RuleBasedClassifier.PatternTestResult testPattern(
            String pattern,
            String text) {

        return classifier.testPattern(pattern, text);
    }

    /**
     * Test a saved rule against text.
     */
    public RuleBasedClassifier.PatternTestResult testRule(
            String ruleId,
            String text) {

        return rulesManager.getRules()
                .stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .map(rule -> classifier.testRule(rule, text))
                .orElse(
                        new RuleBasedClassifier.PatternTestResult(
                                null,
                                false,
                                false,
                                List.of(),
                                "Rule not found: " + ruleId
                        )
                );
    }

    /**
     * Classify arbitrary text without loading a file.
     */
    public RuleBasedClassifier.ClassificationResult classifyText(String text) {

        rulesManager.sortByPriority();

        return classifier.classify(
                rulesManager.getEnabledRules(),
                text
        );
    }

    // ── Batch Processing ──────────────────────────────────────────────────

    /**
     * Auto-label multiple files.
     * Results are returned in the same order as input.
     */
    public List<LabelResult> batchAutoLabel(
            List<String> filePaths,
            BatchProgressCallback callback) {

        List<LabelResult> results = new ArrayList<>();
        int total = filePaths.size();

        for (int i = 0; i < total; i++) {

            String path = filePaths.get(i);
            String name = extractFileName(path);

            if (callback != null) {
                callback.onProgress(i + 1, total, name);
            }

            try {
                results.add(applyAutoLabel(path));

            } catch (Exception e) {

                LabelResult errorResult =
                        new LabelResult(
                                path,
                                name,
                                SecurityLabel.RESTRICTED,
                                LabelResult.Mode.AUTO);

                errorResult.setErrorMessage(
                        "Parse error: " + e.getMessage());

                results.add(errorResult);
            }
        }

        return results;
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    /**
     * Maps classifier match information into LabelResult.
     */
    private void populateMatchDetails(
            LabelResult result,
            RuleBasedClassifier.ClassificationResult classification) {

        for (RuleBasedClassifier.RuleMatchResult matchResult
                : classification.ruleMatches()) {

            LabelRule rule = matchResult.rule();

            result.addMatchedRule(
                    rule.getName()
                            + " ["
                            + rule.getLabel().getDisplayName()
                            + "]");

            if (!matchResult.matches().isEmpty()) {

                result.addMatchedSnippet(
                        "["
                                + rule.getName()
                                + "] "
                                + matchResult.matches()
                                .get(0)
                                .snippet());
            }
        }
    }

    /**
     * Legacy confidence calculation retained for compatibility.
     * Confidence is now calculated by RuleBasedClassifier.
     */
    @SuppressWarnings("unused")
    private double computeConfidence(
            Map<SecurityLabel, Integer> hits,
            SecurityLabel assigned,
            int totalRules) {

        if (totalRules == 0 || assigned == null) {
            return 0.5;
        }

        int matchCount = hits.getOrDefault(assigned, 0);
        int totalHits =
                hits.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

        if (totalHits == 0) {
            return 0.4;
        }

        double base =
                (double) matchCount / totalHits;

        double severityBoost =
                assigned.getSeverity() * 0.03;

        return Math.min(
                0.99,
                base + severityBoost
        );
    }

    private String extractFileName(String filePath) {
        return Paths.get(filePath)
                .getFileName()
                .toString();
    }

    // ── Functional Interface ──────────────────────────────────────────────

    @FunctionalInterface
    public interface BatchProgressCallback {
        void onProgress(
                int current,
                int total,
                String currentFile
        );
    }
}
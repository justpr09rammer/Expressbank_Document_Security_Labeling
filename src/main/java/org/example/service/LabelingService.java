package org.example.service;


import org.example.model.LabelResult;
import org.example.model.LabelRule;
import org.example.model.SecurityLabel;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

/**
 * Core labeling service. Supports both manual and automatic (regex-based) labeling.
 */
public class LabelingService {

    private final DocumentParser parser;
    private final RulesManager   rulesManager;

    /** How many context characters to capture around a match */
    private static final int SNIPPET_CONTEXT = 60;

    public LabelingService(DocumentParser parser, RulesManager rulesManager) {
        this.parser       = parser;
        this.rulesManager = rulesManager;
    }

    // ── Manual Labeling ───────────────────────────────────────────────────

    /**
     * Apply a user-chosen label to a document without any content analysis.
     */
    public LabelResult applyManualLabel(String filePath, SecurityLabel chosenLabel) {
        String fileName = extractFileName(filePath);

        LabelResult result = new LabelResult(filePath, fileName, chosenLabel, LabelResult.Mode.MANUAL);
        result.setConfidenceScore(1.0);   // manual = 100% confidence by definition

        // Still parse to get word count / file size metadata
        try {
            DocumentParser.ParseResult parsed = parser.parse(filePath);
            result.setWordCount(parsed.wordCount());
            result.setFileSizeBytes(parsed.fileSizeBytes());
            result.setFileType(parsed.fileType());
        } catch (IOException | UnsupportedOperationException e) {
            // Non-fatal: metadata unavailable but label is still applied
            result.setWordCount(0);
        }

        return result;
    }

    // ── Automatic Labeling ────────────────────────────────────────────────

    /**
     * Analyse document text with regex rules and assign the highest matching label.
     *
     * @param filePath path to the document
     * @return LabelResult with matched rules, snippets, and confidence score
     * @throws IOException if the file cannot be read
     */
    public LabelResult applyAutoLabel(String filePath) throws IOException {
        String fileName = extractFileName(filePath);

        DocumentParser.ParseResult parsed = parser.parse(filePath);
        String text = parsed.text();

        LabelResult result = new LabelResult(filePath, fileName, null, LabelResult.Mode.AUTO);
        result.setWordCount(parsed.wordCount());
        result.setFileSizeBytes(parsed.fileSizeBytes());
        result.setFileType(parsed.fileType());

        // Maps label → count of rules that matched it
        Map<SecurityLabel, Integer> labelHits   = new EnumMap<>(SecurityLabel.class);
        Map<SecurityLabel, Integer> labelWeight  = new EnumMap<>(SecurityLabel.class);

        List<LabelRule> enabledRules = rulesManager.getEnabledRules();
        rulesManager.sortByPriority();

        for (LabelRule rule : enabledRules) {
            try {
                Pattern p = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher m = p.matcher(text);

                if (m.find()) {
                    // Record the rule match
                    result.addMatchedRule(rule.getName() + " [" + rule.getLabel().getDisplayName() + "]");

                    // Capture a context snippet
                    int start   = Math.max(0, m.start() - SNIPPET_CONTEXT);
                    int end     = Math.min(text.length(), m.end() + SNIPPET_CONTEXT);
                    String snip = "…" + text.substring(start, end).replace("\n", " ").trim() + "…";
                    result.addMatchedSnippet("[" + rule.getName() + "] " + snip);

                    // Accumulate hits and weighted severity
                    labelHits.merge(rule.getLabel(), 1, Integer::sum);
                    labelWeight.merge(rule.getLabel(), rule.getPriority(), Integer::sum);
                }
            } catch (PatternSyntaxException e) {
                System.err.println("[LabelingService] Skipping invalid rule pattern '"
                    + rule.getPattern() + "': " + e.getMessage());
            }
        }

        // Pick label by highest total weight (severity × hits)
        SecurityLabel assignedLabel = null;
        int           maxWeight     = 0;

        for (Map.Entry<SecurityLabel, Integer> entry : labelWeight.entrySet()) {
            if (entry.getValue() > maxWeight) {
                maxWeight     = entry.getValue();
                assignedLabel = entry.getKey();
            }
        }

        // Default to INTERNAL_ONLY if nothing matched but file has content
        if (assignedLabel == null && !text.isBlank()) {
            assignedLabel = SecurityLabel.INTERNAL_ONLY;
        } else if (assignedLabel == null) {
            assignedLabel = SecurityLabel.PUBLIC;
        }

        result.setLabel(assignedLabel);
        result.setConfidenceScore(computeConfidence(labelHits, assignedLabel, enabledRules.size()));

        return result;
    }

    // ── Batch Processing ──────────────────────────────────────────────────

    /**
     * Auto-label multiple files. Returns results in the same order as the input list.
     * Individual failures are recorded as error results (not thrown).
     */
    public List<LabelResult> batchAutoLabel(List<String> filePaths,
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
                LabelResult err = new LabelResult(path, name, SecurityLabel.RESTRICTED, LabelResult.Mode.AUTO);
                err.setErrorMessage("Parse error: " + e.getMessage());
                results.add(err);
            }
        }
        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Confidence = ratio of (rules that matched the assigned label) to (total enabled rules),
     * weighted by label severity. Capped at 0.99 for auto mode.
     */
    private double computeConfidence(Map<SecurityLabel, Integer> hits,
                                     SecurityLabel assigned,
                                     int totalRules) {
        if (totalRules == 0 || assigned == null) return 0.5;

        int matchCount = hits.getOrDefault(assigned, 0);
        int totalHits  = hits.values().stream().mapToInt(Integer::intValue).sum();

        if (totalHits == 0) return 0.4;   // default label, no matches

        // Base score: what fraction of all hits belonged to the winning label
        double base = (double) matchCount / totalHits;

        // Boost by severity: restricted matches get extra confidence
        double severityBoost = assigned.getSeverity() * 0.03;

        return Math.min(0.99, base + severityBoost);
    }

    private String extractFileName(String filePath) {
        return java.nio.file.Paths.get(filePath).getFileName().toString();
    }

    // ── Functional Interface ──────────────────────────────────────────────

    @FunctionalInterface
    public interface BatchProgressCallback {
        void onProgress(int current, int total, String currentFile);
    }
}
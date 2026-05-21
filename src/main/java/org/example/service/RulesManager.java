package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.LabelRule;
import org.example.model.SecurityLabel;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RulesManager {

    private static final String RULES_FILE = System.getProperty("user.home")
        + File.separator + ".securitylabeler" + File.separator + "rules.json";

    private final ObjectMapper mapper;
    private List<LabelRule>   rules;

    public RulesManager() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.rules  = new ArrayList<>();
    }

    public void load() {
        Path path = Paths.get(RULES_FILE);
        if (Files.exists(path)) {
            try {
                rules = mapper.readValue(path.toFile(),
                    new TypeReference<List<LabelRule>>() {});
                System.out.println("[RulesManager] Loaded " + rules.size() + " rules from " + path);
                return;
            } catch (IOException e) {
                System.err.println("[RulesManager] Failed to load rules, using defaults: " + e.getMessage());
            }
        }
        loadDefaults();
    }

    /**
     * Save current rules to disk.
     */
    public void save() throws IOException {
        Path path = Paths.get(RULES_FILE);
        Files.createDirectories(path.getParent());
        mapper.writeValue(path.toFile(), rules);
        System.out.println("[RulesManager] Saved " + rules.size() + " rules to " + path);
    }

    // ── Built-in Default Rules ────────────────────────────────────────────

    private void loadDefaults() {
        rules.clear();

        // ── RESTRICTED rules ─────────────────────────────────────────────
        rules.add(new LabelRule(
            "Trade Secrets",
            "(?i)\\b(trade secret|proprietary formula|classified|top secret|eyes only)\\b",
            SecurityLabel.RESTRICTED,
            "Documents referencing classified or trade-secret content"
        ));
        rules.add(new LabelRule(
            "Critical Infrastructure",
            "(?i)\\b(critical infrastructure|nuclear|weapons|explosive|classified project)\\b",
            SecurityLabel.RESTRICTED,
            "References to critical infrastructure or classified projects"
        ));
        rules.add(new LabelRule(
            "Cryptographic Keys",
            "(?i)(-----BEGIN (RSA|EC|PGP|OPENSSH) (PRIVATE|PUBLIC) KEY-----|api.?key\\s*[:=]\\s*['\"]?[A-Za-z0-9+/]{20,})",
            SecurityLabel.RESTRICTED,
            "Documents containing cryptographic keys or API secrets"
        ));

        // ── CONFIDENTIAL rules ────────────────────────────────────────────
        rules.add(new LabelRule(
            "Confidential Marker",
            "(?i)\\b(confidential|strictly confidential|private and confidential|do not distribute)\\b",
            SecurityLabel.CONFIDENTIAL,
            "Explicit 'confidential' markers in text"
        ));
        rules.add(new LabelRule(
            "Personal Identifiable Info",
            "(?i)\\b(date of birth|social security|passport number|national id|driver.?s? license|medical record)\\b",
            SecurityLabel.CONFIDENTIAL,
            "Documents containing personal identifiable information (PII)"
        ));
        rules.add(new LabelRule(
            "Financial Records",
            "(?i)\\b(financial record|bank account|credit card|iban|swift code|salary|payroll|tax return)\\b",
            SecurityLabel.CONFIDENTIAL,
            "Documents containing financial records or account information"
        ));
        rules.add(new LabelRule(
            "Legal / Contract",
            "(?i)\\b(non.?disclosure agreement|nda|attorney.?client|privileged|legal advice|litigation)\\b",
            SecurityLabel.CONFIDENTIAL,
            "Legal documents, NDAs, or privileged communications"
        ));
        rules.add(new LabelRule(
            "Secret / Sensitive Keyword",
            "(?i)\\b(secret|sensitive|restricted access|not for distribution)\\b",
            SecurityLabel.CONFIDENTIAL,
            "General sensitive-content keywords"
        ));

        // ── INTERNAL ONLY rules ───────────────────────────────────────────
        rules.add(new LabelRule(
            "Internal Use",
            "(?i)\\b(internal use only|internal only|for internal use|employees only|staff only)\\b",
            SecurityLabel.INTERNAL_ONLY,
            "Explicit 'internal use only' markers"
        ));
        rules.add(new LabelRule(
            "HR / Employee",
            "(?i)\\b(performance review|employee handbook|org chart|headcount|onboarding|offboarding)\\b",
            SecurityLabel.INTERNAL_ONLY,
            "HR and employee-related documents"
        ));
        rules.add(new LabelRule(
            "Internal Meeting",
            "(?i)\\b(meeting minutes|action items|agenda|internal memo|board meeting|quarterly review)\\b",
            SecurityLabel.INTERNAL_ONLY,
            "Internal meetings, memos, and agendas"
        ));

        // ── PUBLIC rules ──────────────────────────────────────────────────
        rules.add(new LabelRule(
            "Press Release",
            "(?i)\\b(press release|for immediate release|media release|public announcement)\\b",
            SecurityLabel.PUBLIC,
            "Press releases and public announcements"
        ));
        rules.add(new LabelRule(
            "Marketing / Promotional",
            "(?i)\\b(marketing material|promotional|advertisement|brochure|product launch|public website)\\b",
            SecurityLabel.PUBLIC,
            "Marketing and promotional content"
        ));

        System.out.println("[RulesManager] Loaded " + rules.size() + " default rules");
    }


    public List<LabelRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public List<LabelRule> getEnabledRules() {
        return rules.stream().filter(LabelRule::isEnabled).toList();
    }

    public void addRule(LabelRule rule) {
        rules.add(rule);
    }

    public void updateRule(LabelRule updated) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(updated.getId())) {
                rules.set(i, updated);
                return;
            }
        }
    }

    public void removeRule(String id) {
        rules.removeIf(r -> r.getId().equals(id));
    }

    public void resetToDefaults() {
        loadDefaults();
    }

    public void sortByPriority() {
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
}
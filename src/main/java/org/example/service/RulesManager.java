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

        // ─────────────────────────────────────────────────────────────
        // RESTRICTED
        // Highly sensitive / critical information
        // ─────────────────────────────────────────────────────────────

        rules.add(new LabelRule(
                "Private Keys",
                "(?i)(-----BEGIN (RSA|DSA|EC|OPENSSH|PGP) PRIVATE KEY-----)",
                SecurityLabel.RESTRICTED,
                "Detected cryptographic private keys"
        ));

        rules.add(new LabelRule(
                "API Keys / Tokens",
                "(?i)\\b(api[_-]?key|secret[_-]?key|access[_-]?token|auth[_-]?token)\\b\\s*[:=]\\s*['\"]?[A-Za-z0-9\\-_]{16,}",
                SecurityLabel.RESTRICTED,
                "Detected API keys or authentication tokens"
        ));

        rules.add(new LabelRule(
                "AWS Credentials",
                "(?i)(AKIA[0-9A-Z]{16}|aws_secret_access_key)",
                SecurityLabel.RESTRICTED,
                "Detected AWS credentials"
        ));

        rules.add(new LabelRule(
                "Database Connection String",
                "(?i)(jdbc:mysql://|jdbc:postgresql://|mongodb://|Server=.*;Database=.*;User Id=.*;Password=.*)",
                SecurityLabel.RESTRICTED,
                "Detected database connection credentials"
        ));

        rules.add(new LabelRule(
                "Passwords",
                "(?i)\\b(password|passwd|pwd)\\b\\s*[:=]\\s*['\"]?.{4,}",
                SecurityLabel.RESTRICTED,
                "Detected passwords in plain text"
        ));

        // ─────────────────────────────────────────────────────────────
        // CONFIDENTIAL
        // Personal, financial, or sensitive company data
        // ─────────────────────────────────────────────────────────────

        rules.add(new LabelRule(
                "Credit Card Numbers",
                "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected possible credit card numbers"
        ));

        rules.add(new LabelRule(
                "CVV Codes",
                "\\bCVV\\s*[:=]?\\s*[0-9]{3,4}\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected card CVV codes"
        ));

        rules.add(new LabelRule(
                "IBAN Numbers",
                "\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected IBAN bank account numbers"
        ));

        rules.add(new LabelRule(
                "SWIFT Codes",
                "\\b[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected SWIFT/BIC banking codes"
        ));

        rules.add(new LabelRule(
                "Social Security Numbers",
                "\\b\\d{3}-\\d{2}-\\d{4}\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected social security numbers"
        ));

        rules.add(new LabelRule(
                "Phone Numbers",
                "\\b(\\+\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected phone numbers"
        ));

        rules.add(new LabelRule(
                "Email Addresses",
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected email addresses"
        ));

        rules.add(new LabelRule(
                "Passport Numbers",
                "(?i)\\b(passport number|passport no|passport)\\b\\s*[:=]?\\s*[A-Z0-9]{6,9}",
                SecurityLabel.CONFIDENTIAL,
                "Detected passport information"
        ));

        rules.add(new LabelRule(
                "National ID Numbers",
                "(?i)\\b(national id|identity number|ssn|tin)\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected government-issued identification"
        ));

        rules.add(new LabelRule(
                "Salary / Payroll",
                "(?i)\\b(salary|payroll|bonus|compensation|tax return)\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected salary or payroll information"
        ));

        rules.add(new LabelRule(
                "Confidential Keywords",
                "(?i)\\b(confidential|strictly confidential|private and confidential|do not distribute|sensitive information)\\b",
                SecurityLabel.CONFIDENTIAL,
                "Detected confidential document markers"
        ));

        // ─────────────────────────────────────────────────────────────
        // INTERNAL ONLY
        // Internal business information
        // ─────────────────────────────────────────────────────────────

        rules.add(new LabelRule(
                "Internal Use",
                "(?i)\\b(internal use only|internal only|employees only|staff only)\\b",
                SecurityLabel.INTERNAL_ONLY,
                "Detected internal-use-only markers"
        ));

        rules.add(new LabelRule(
                "Meeting Documents",
                "(?i)\\b(meeting minutes|agenda|action items|internal memo|quarterly review)\\b",
                SecurityLabel.INTERNAL_ONLY,
                "Detected internal meeting documents"
        ));

        rules.add(new LabelRule(
                "HR Documents",
                "(?i)\\b(employee handbook|performance review|onboarding|offboarding|org chart)\\b",
                SecurityLabel.INTERNAL_ONLY,
                "Detected HR-related documents"
        ));

        rules.add(new LabelRule(
                "Source Code References",
                "(?i)\\b(github|gitlab|repository|source code|internal repo)\\b",
                SecurityLabel.INTERNAL_ONLY,
                "Detected software development references"
        ));

        // ─────────────────────────────────────────────────────────────
        // PUBLIC
        // Publicly shareable content
        // ─────────────────────────────────────────────────────────────

        rules.add(new LabelRule(
                "Press Release",
                "(?i)\\b(press release|for immediate release|media release)\\b",
                SecurityLabel.PUBLIC,
                "Detected press release content"
        ));

        rules.add(new LabelRule(
                "Marketing Content",
                "(?i)\\b(marketing material|brochure|advertisement|product launch|public website)\\b",
                SecurityLabel.PUBLIC,
                "Detected marketing or promotional material"
        ));

        rules.add(new LabelRule(
                "Public Announcement",
                "(?i)\\b(public announcement|newsletter|public information)\\b",
                SecurityLabel.PUBLIC,
                "Detected public-facing content"
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
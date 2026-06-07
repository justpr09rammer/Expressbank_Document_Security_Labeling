package org.example.utils;

import org.example.model.LabelResult;
import org.example.model.LabelRule;
import org.example.model.SecurityLabel;
import org.example.service.DocumentParser;
import org.example.service.LabelingService;
import org.example.service.RuleBasedClassifier;
import org.example.service.RulesManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainWindow {

    // ── Services ──────────────────────────────────────────────────────────
    private final LabelingService labelingService;
    private final RulesManager    rulesManager;

    // ── Top-level frame ───────────────────────────────────────────────────
    private JFrame frame;

    // ── File labeling panel ───────────────────────────────────────────────
    private JTextField            fileField;
    private JComboBox<SecurityLabel> labelDropdown;
    private JTextArea             resultArea;

    // ── Regex tester panel ────────────────────────────────────────────────
    private JTextField  rtPatternField;
    private JTextArea   rtInputArea;
    private JTextArea   rtOutputArea;

    // ── Rule match panel ──────────────────────────────────────────────────
    private JComboBox<LabelRule> ruleCombo;
    private JTextArea            rmInputArea;
    private JTextArea            rmOutputArea;

    // ── Classify text panel ───────────────────────────────────────────────
    private JTextArea ctInputArea;
    private JTextArea ctOutputArea;

    // ─────────────────────────────────────────────────────────────────────

    public MainWindow() {
        DocumentParser parser = new DocumentParser();
        rulesManager = new RulesManager();
        rulesManager.load();
        labelingService = new LabelingService(parser, rulesManager);
        createUI();
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI construction
    // ═════════════════════════════════════════════════════════════════════

    private void createUI() {
        frame = new JFrame("Document Security Labeler");
        frame.setSize(900, 680);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("📄 Label File",       buildLabelFilePanel());
        tabs.addTab("🔍 Regex Tester",     buildRegexTesterPanel());
        tabs.addTab("🛡️ Rule Match",       buildRuleMatchPanel());
        tabs.addTab("📝 Classify Text",    buildClassifyTextPanel());

        frame.add(tabs, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // ─── Tab 1: Label File ────────────────────────────────────────────────

    private JPanel buildLabelFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // File picker row
        JPanel fileRow = new JPanel(new BorderLayout(5, 0));
        fileField = new JTextField();
        fileField.setEditable(false);
        JButton browseBtn = new JButton("Browse…");
        browseBtn.addActionListener(e -> selectFile());
        fileRow.add(new JLabel("File: "), BorderLayout.WEST);
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browseBtn, BorderLayout.EAST);

        // Action row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setBorder(BorderFactory.createTitledBorder("Labeling Options"));
        JButton autoBtn = new JButton("Auto Label");
        autoBtn.addActionListener(e -> autoLabel());
        labelDropdown = new JComboBox<>(SecurityLabel.values());
        JButton manualBtn = new JButton("Manual Label");
        manualBtn.addActionListener(e -> manualLabel());
        actionRow.add(autoBtn);
        actionRow.add(new JLabel("  Label:"));
        actionRow.add(labelDropdown);
        actionRow.add(manualBtn);

        // Result area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Results"));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 6));
        top.add(fileRow);
        top.add(actionRow);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ─── Tab 2: Regex Tester ──────────────────────────────────────────────

    private JPanel buildRegexTesterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Pattern input row
        JPanel patternRow = new JPanel(new BorderLayout(5, 0));
        rtPatternField = new JTextField();
        rtPatternField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        rtPatternField.setToolTipText("Enter any Java regex pattern");
        JButton testBtn = new JButton("Test ▶");
        testBtn.addActionListener(e -> runRegexTest());
        patternRow.add(new JLabel("Pattern: "), BorderLayout.WEST);
        patternRow.add(rtPatternField, BorderLayout.CENTER);
        patternRow.add(testBtn, BorderLayout.EAST);

        // Split: input top, output bottom
        rtInputArea  = monospaceArea("Paste text to test against the pattern…");
        rtOutputArea = monospaceArea("");
        rtOutputArea.setEditable(false);
        rtOutputArea.setBackground(new Color(245, 245, 245));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                titledScroll("Test Input", rtInputArea),
                titledScroll("Match Results", rtOutputArea));
        split.setResizeWeight(0.45);
        split.setDividerSize(6);

        // Live test on type
        rtPatternField.getDocument().addDocumentListener(simpleListener(this::runRegexTest));
        rtInputArea.getDocument().addDocumentListener(simpleListener(this::runRegexTest));

        panel.add(patternRow, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel("  Tip: supports full Java regex — (?i) flag, lookaheads, groups, etc.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(Color.GRAY);
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    // ─── Tab 3: Rule Match ────────────────────────────────────────────────

    private JPanel buildRuleMatchPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Rule selector row
        JPanel ruleRow = new JPanel(new BorderLayout(5, 0));
        ruleCombo = new JComboBox<>();
        refreshRuleCombo();
        JButton runBtn = new JButton("Run Rule ▶");
        runBtn.addActionListener(e -> runRuleMatch());
        ruleRow.add(new JLabel("Rule: "), BorderLayout.WEST);
        ruleRow.add(ruleCombo, BorderLayout.CENTER);
        ruleRow.add(runBtn, BorderLayout.EAST);

        rmInputArea  = monospaceArea("Paste text to check against the selected rule…");
        rmOutputArea = monospaceArea("");
        rmOutputArea.setEditable(false);
        rmOutputArea.setBackground(new Color(245, 245, 245));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                titledScroll("Test Input", rmInputArea),
                titledScroll("Rule Match Result", rmOutputArea));
        split.setResizeWeight(0.45);
        split.setDividerSize(6);

        rmInputArea.getDocument().addDocumentListener(simpleListener(this::runRuleMatch));

        panel.add(ruleRow, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ─── Tab 4: Classify Text ─────────────────────────────────────────────

    private JPanel buildClassifyTextPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton classifyBtn = new JButton("Classify ▶");
        classifyBtn.addActionListener(e -> runClassifyText());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> { ctInputArea.setText(""); ctOutputArea.setText(""); });
        btnRow.add(classifyBtn);
        btnRow.add(clearBtn);

        ctInputArea  = monospaceArea("Paste document text here to classify using all enabled rules…");
        ctOutputArea = monospaceArea("");
        ctOutputArea.setEditable(false);
        ctOutputArea.setBackground(new Color(245, 245, 245));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                titledScroll("Document Text", ctInputArea),
                titledScroll("Classification Result", ctOutputArea));
        split.setResizeWeight(0.45);
        split.setDividerSize(6);

        ctInputArea.getDocument().addDocumentListener(simpleListener(this::runClassifyText));

        panel.add(btnRow, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tab 1 actions
    // ═════════════════════════════════════════════════════════════════════

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fileField.setText(chooser.getSelectedFile().getAbsolutePath());
            resultArea.setText("");
        }
    }

    private void autoLabel() {
        String path = fileField.getText().trim();
        if (path.isEmpty()) { showError("Please select a file first."); return; }
        try {
            resultArea.setText("Processing…\n");
            displayResult(labelingService.applyAutoLabel(path));
        } catch (Exception e) {
            resultArea.setText("❌ Auto labeling failed: " + e.getMessage());
        }
    }

    private void manualLabel() {
        String path = fileField.getText().trim();
        if (path.isEmpty()) { showError("Please select a file first."); return; }
        SecurityLabel sel = (SecurityLabel) labelDropdown.getSelectedItem();
        try {
            displayResult(labelingService.applyManualLabel(path, sel));
        } catch (Exception e) {
            resultArea.setText("❌ Manual labeling failed: " + e.getMessage());
        }
    }

    private void displayResult(LabelResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║           LABELING RESULT                ║\n");
        sb.append("╚══════════════════════════════════════════╝\n\n");
        sb.append("📄 File:        ").append(r.getFileName()).append("\n");
        sb.append("🏷️  Label:       ").append(r.getLabel()).append("\n");
        sb.append("⚙️  Mode:        ").append(r.getMode()).append("\n");
        sb.append("📊 Confidence:  ").append(r.getConfidencePercent()).append("\n");
        sb.append("📈 Word Count:  ").append(r.getWordCount()).append("\n");
        sb.append("💾 File Size:   ").append(r.getLongFileSizeBytes()).append(" bytes\n");
        sb.append("📝 File Type:   ").append(r.getFileType()).append("\n");

        if (r.getMode() == LabelResult.Mode.AUTO) {
            sb.append("\n═══════════════════════════════════════════\n");
            sb.append("MATCHED RULES:\n");
            sb.append("═══════════════════════════════════════════\n");
            if (r.getMatchedRules().isEmpty()) {
                sb.append("  (no rules matched — default label applied)\n");
            } else {
                r.getMatchedRules().forEach(rule -> sb.append("  ✓ ").append(rule).append("\n"));
            }
            sb.append("\n═══════════════════════════════════════════\n");
            sb.append("CONTEXT SNIPPETS:\n");
            sb.append("═══════════════════════════════════════════\n");
            if (r.getMatchedSnippets().isEmpty()) {
                sb.append("  (no snippets)\n");
            } else {
                r.getMatchedSnippets().forEach(s -> sb.append("  → ").append(s).append("\n"));
            }
        }
        sb.append("\n✅ Done.");
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tab 2: Regex Tester
    // ═════════════════════════════════════════════════════════════════════

    private void runRegexTest() {
        String pattern = rtPatternField.getText().trim();
        String text    = rtInputArea.getText();

        if (pattern.isEmpty()) { rtOutputArea.setText("Enter a regex pattern above."); return; }

        RuleBasedClassifier.PatternTestResult result =
                labelingService.testPattern(pattern, text);

        StringBuilder sb = new StringBuilder();
        if (!result.valid()) {
            sb.append("❌ INVALID PATTERN\n").append(result.errorMessage());
        } else if (!result.matched()) {
            sb.append("⚪ NO MATCH\n\nThe pattern did not match any part of the test string.");
        } else {
            List<RuleBasedClassifier.MatchDetail> matches = result.matches();
            sb.append("✅ MATCHED — ")
                    .append(matches.size()).append(" occurrence")
                    .append(matches.size() == 1 ? "" : "s").append("\n");
            sb.append("Pattern: ").append(pattern).append("\n");
            sb.append("─".repeat(50)).append("\n\n");
            int n = 1;
            for (RuleBasedClassifier.MatchDetail d : matches) {
                sb.append("Match #").append(n++).append("\n");
                sb.append("  Value : ").append(d.matchedText()).append("\n");
                sb.append("  Pos   : ").append(d.startIndex())
                        .append("–").append(d.endIndex()).append("\n");
                sb.append("  Context: ").append(d.snippet()).append("\n\n");
            }
        }
        rtOutputArea.setText(sb.toString());
        rtOutputArea.setCaretPosition(0);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tab 3: Rule Match
    // ═════════════════════════════════════════════════════════════════════

    private void refreshRuleCombo() {
        ruleCombo.removeAllItems();
        rulesManager.getRules().forEach(ruleCombo::addItem);
    }

    private void runRuleMatch() {
        LabelRule selected = (LabelRule) ruleCombo.getSelectedItem();
        if (selected == null) { rmOutputArea.setText("No rules available."); return; }
        String text = rmInputArea.getText();

        RuleBasedClassifier.PatternTestResult result =
                labelingService.testRule(selected.getId(), text);

        StringBuilder sb = new StringBuilder();
        sb.append("Rule    : ").append(selected.getName()).append("\n");
        sb.append("Label   : ").append(selected.getLabel().getDisplayName()).append("\n");
        sb.append("Pattern : ").append(selected.getPattern()).append("\n");
        sb.append("Enabled : ").append(selected.isEnabled() ? "yes" : "no").append("\n");
        sb.append("─".repeat(50)).append("\n\n");

        if (!result.valid()) {
            sb.append("❌ Invalid pattern in rule: ").append(result.errorMessage());
        } else if (!result.matched()) {
            sb.append("⚪ NOT MATCHED\n\nThis rule did not trigger on the provided text.");
        } else {
            sb.append("✅ MATCHED — ")
                    .append(result.matches().size()).append(" time")
                    .append(result.matches().size() == 1 ? "" : "s").append("\n\n");
            int n = 1;
            for (RuleBasedClassifier.MatchDetail d : result.matches()) {
                sb.append("  #").append(n++).append("  \"").append(d.matchedText())
                        .append("\"  (pos ").append(d.startIndex()).append("–")
                        .append(d.endIndex()).append(")\n");
                sb.append("       ").append(d.snippet()).append("\n\n");
            }
        }
        rmOutputArea.setText(sb.toString());
        rmOutputArea.setCaretPosition(0);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Tab 4: Classify Text
    // ═════════════════════════════════════════════════════════════════════

    private void runClassifyText() {
        String text = ctInputArea.getText().trim();
        if (text.isEmpty()) { ctOutputArea.setText("Paste text above to classify."); return; }

        RuleBasedClassifier.ClassificationResult cr =
                labelingService.classifyText(text);

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════╗\n");
        sb.append("║         CLASSIFICATION RESULT            ║\n");
        sb.append("╚══════════════════════════════════════════╝\n\n");
        sb.append("🏷️  Assigned Label : ").append(cr.assignedLabel().getDisplayName()).append("\n");
        sb.append("📊 Confidence     : ")
                .append(String.format("%.0f%%", cr.confidenceScore() * 100)).append("\n");
        sb.append("📋 Rules checked  : ").append(cr.totalRulesChecked()).append("\n");
        sb.append("✅ Rules matched  : ").append(cr.ruleMatches().size()).append("\n\n");

        if (cr.ruleMatches().isEmpty()) {
            sb.append("  No rules matched — default label applied.\n");
        } else {
            sb.append("═══════════════════════════════════════════\n");
            sb.append("MATCHED RULES & SNIPPETS:\n");
            sb.append("═══════════════════════════════════════════\n\n");
            for (RuleBasedClassifier.RuleMatchResult rm : cr.ruleMatches()) {
                sb.append("✓ ").append(rm.rule().getName())
                        .append("  [").append(rm.rule().getLabel().getDisplayName()).append("]\n");
                for (RuleBasedClassifier.MatchDetail d : rm.matches()) {
                    sb.append("    → \"").append(d.matchedText()).append("\"\n");
                    sb.append("      ").append(d.snippet()).append("\n");
                }
                sb.append("\n");
            }
        }
        ctOutputArea.setText(sb.toString());
        ctOutputArea.setCaretPosition(0);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════

    private JTextArea monospaceArea(String placeholder) {
        JTextArea ta = new JTextArea(placeholder);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    private JScrollPane titledScroll(String title, JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    private javax.swing.event.DocumentListener simpleListener(Runnable action) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { action.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { action.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        };
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
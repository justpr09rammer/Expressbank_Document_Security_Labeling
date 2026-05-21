package org.example.utils;

import org.example.model.LabelResult;
import org.example.model.SecurityLabel;
import org.example.service.DocumentParser;
import org.example.service.LabelingService;
import org.example.service.RulesManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainWindow {

    private JFrame frame;
    private JTextField fileField;
    private JTextArea resultArea;
    private JComboBox<SecurityLabel> labelDropdown;

    private final LabelingService labelingService;

    public MainWindow() {
        // Init services
        DocumentParser parser = new DocumentParser();
        RulesManager rulesManager = new RulesManager();
        rulesManager.load();

        labelingService = new LabelingService(parser, rulesManager);

        createUI();
    }

    private void createUI() {
        frame = new JFrame("Document Security Labeler");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        fileField = new JTextField();
        fileField.setEditable(false);

        JButton browseBtn = new JButton("Select File");
        browseBtn.addActionListener(e -> selectFile());

        topPanel.add(new JLabel("File: "), BorderLayout.WEST);
        topPanel.add(fileField, BorderLayout.CENTER);
        topPanel.add(browseBtn, BorderLayout.EAST);

        JPanel middlePanel = new JPanel();
        middlePanel.setBorder(BorderFactory.createTitledBorder("Labeling Options"));

        JButton autoBtn = new JButton("Auto Label");
        autoBtn.addActionListener(e -> autoLabel());

        labelDropdown = new JComboBox<>(SecurityLabel.values());

        JButton manualBtn = new JButton("Manual Label");
        manualBtn.addActionListener(e -> manualLabel());

        middlePanel.add(autoBtn);
        middlePanel.add(new JLabel(" Label: "));
        middlePanel.add(labelDropdown);
        middlePanel.add(manualBtn);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Results"));

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(middlePanel, BorderLayout.CENTER);
        frame.add(scroll, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // ─── FILE PICKER ───────────────────────────────────────────
    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            fileField.setText(file.getAbsolutePath());
            resultArea.setText(""); // Clear previous results
        }
    }

    private void autoLabel() {
        String path = fileField.getText();

        if (path.isEmpty()) {
            showError("Please select a file first!");
            return;
        }

        try {
            resultArea.setText("Processing... Please wait.\n");
            LabelResult result = labelingService.applyAutoLabel(path);
            displayResult(result);
        } catch (Exception e) {
            showError("Auto labeling failed: " + e.getMessage());
            resultArea.setText("❌ Auto labeling failed: " + e.getMessage());
        }
    }

    // ─── MANUAL LABEL ──────────────────────────────────────────
    private void manualLabel() {
        String path = fileField.getText();

        if (path.isEmpty()) {
            showError("Please select a file first!");
            return;
        }

        SecurityLabel selected = (SecurityLabel) labelDropdown.getSelectedItem();

        try {
            LabelResult result = labelingService.applyManualLabel(path, selected);
            displayResult(result);
        } catch (Exception e) {
            showError("Manual labeling failed: " + e.getMessage());
            resultArea.setText("❌ Manual labeling failed: " + e.getMessage());
        }
    }

    private void displayResult(LabelResult r) {
        StringBuilder sb = new StringBuilder();

        // ═══════════════════════════════════════════════════════════
        sb.append("╔════════════════════════════════════════════════╗\n");
        sb.append("║          LABELING RESULT                       ║\n");
        sb.append("╚════════════════════════════════════════════════╝\n\n");

        sb.append("📄 File: ").append(r.getFileName()).append("\n");
        sb.append("🏷️  Label: ").append(r.getLabel()).append("\n");
        sb.append("⚙️  Mode: ").append(r.getMode()).append("\n");
        sb.append("📊 Confidence: ").append(String.format("%.1f", Double.parseDouble(r.getConfidencePercent().replace("%", "")))).append("%\n");
        sb.append("📈 Word Count: ").append(r.getWordCount()).append("\n");
        sb.append("💾 File Size: ").append(r.getLongFileSizeBytes()).append(" bytes\n");
        sb.append("📝 File Type: ").append(r.getFileType()).append("\n");

        if (r.getMode() == LabelResult.Mode.AUTO) {
            sb.append("\n");
            sb.append("═══════════════════════════════════════════════\n");
            sb.append("MATCHED RULES:\n");
            sb.append("═══════════════════════════════════════════════\n");

            if (r.getMatchedRules().isEmpty()) {
                sb.append("✓ No rules matched - using default label\n");
            } else {
                for (String rule : r.getMatchedRules()) {
                    sb.append("✓ ").append(rule).append("\n");
                }
            }

            sb.append("\n");
            sb.append("═══════════════════════════════════════════════\n");
            sb.append("CONTEXT SNIPPETS:\n");
            sb.append("═══════════════════════════════════════════════\n");

            if (r.getMatchedSnippets().isEmpty()) {
                sb.append("(No snippets found)\n");
            } else {
                for (String s : r.getMatchedSnippets()) {
                    sb.append("→ ").append(s).append("\n");
                }
            }
        }

        sb.append("\n✅ Labeling completed successfully!");
        resultArea.setText(sb.toString());

        // Scroll to top
        resultArea.setCaretPosition(0);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}

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
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        fileField = new JTextField();
        fileField.setEditable(false);

        JButton browseBtn = new JButton("Select File");
        browseBtn.addActionListener(e -> selectFile());

        topPanel.add(fileField, BorderLayout.CENTER);
        topPanel.add(browseBtn, BorderLayout.EAST);

        JPanel middlePanel = new JPanel();

        JButton autoBtn = new JButton("Auto Label");
        autoBtn.addActionListener(e -> autoLabel());

        labelDropdown = new JComboBox<>(SecurityLabel.values());

        JButton manualBtn = new JButton("Manual Label");
        manualBtn.addActionListener(e -> manualLabel());

        middlePanel.add(autoBtn);
        middlePanel.add(labelDropdown);
        middlePanel.add(manualBtn);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(resultArea);

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
        }
    }

    private void autoLabel() {
        String path = fileField.getText();

        if (path.isEmpty()) {
            showError("Please select a file first!");
            return;
        }

        try {
            LabelResult result = labelingService.applyAutoLabel(path);
            displayResult(result);
        } catch (Exception e) {
            showError("Auto labeling failed: " + e.getMessage());
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

        LabelResult result = labelingService.applyManualLabel(path, selected);
        displayResult(result);
    }

    private void displayResult(LabelResult r) {
        StringBuilder sb = new StringBuilder();

        sb.append("File: ").append(r.getFileName()).append("\n");
        sb.append("Label: ").append(r.getLabel()).append("\n");
        sb.append("Mode: ").append(r.getMode()).append("\n");
        sb.append("Confidence: ").append(r.getConfidencePercent()).append("\n");
        sb.append("Word count: ").append(r.getWordCount()).append("\n");

        if (r.getMode() == LabelResult.Mode.AUTO) {
            sb.append("\nMatched Rules:\n");
            for (String rule : r.getMatchedRules()) {
                sb.append("- ").append(rule).append("\n");
            }

            sb.append("\nSnippets:\n");
            for (String s : r.getMatchedSnippets()) {
                sb.append("- ").append(s).append("\n");
            }
        }

        resultArea.setText(sb.toString());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
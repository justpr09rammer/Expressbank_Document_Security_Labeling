package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of a labeling operation for one document.
 */
public class LabelResult {

    public enum Mode { MANUAL, AUTO }

    private String         filePath;
    private String         fileName;
    private String         fileType;          // e.g. "docx", "xlsx"
    private SecurityLabel  label;
    private Mode           mode;
    private LocalDateTime  timestamp;
    private List<String>   matchedRules;      // names of triggered rules (auto mode)
    private List<String>   matchedSnippets;   // text snippets that triggered rules
    private double         confidenceScore;   // 0.0 – 1.0
    private long           fileSizeBytes;
    private int            wordCount;
    private String         errorMessage;      // non-null if parsing failed

    public LabelResult() {
        this.matchedRules    = new ArrayList<>();
        this.matchedSnippets = new ArrayList<>();
        this.timestamp       = LocalDateTime.now();
    }

    public LabelResult(String filePath, String fileName, SecurityLabel label, Mode mode) {
        this();
        this.filePath = filePath;
        this.fileName = fileName;
        this.label    = label;
        this.mode     = mode;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getFilePath()                          { return filePath; }
    public void   setFilePath(String filePath)           { this.filePath = filePath; }

    public String getFileName()                          { return fileName; }
    public void   setFileName(String fileName)           { this.fileName = fileName; }

    public String getFileType()                          { return fileType; }
    public void   setFileType(String fileType)           { this.fileType = fileType; }

    public SecurityLabel getLabel()                      { return label; }
    public void          setLabel(SecurityLabel label)   { this.label = label; }

    public Mode getMode()                                { return mode; }
    public void setMode(Mode mode)                       { this.mode = mode; }

    public LocalDateTime getTimestamp()                  { return timestamp; }
    public void          setTimestamp(LocalDateTime ts)  { this.timestamp = ts; }

    public List<String> getMatchedRules()                { return matchedRules; }
    public void         setMatchedRules(List<String> r)  { this.matchedRules = r; }
    public void         addMatchedRule(String rule)      { this.matchedRules.add(rule); }

    public List<String> getMatchedSnippets()             { return matchedSnippets; }
    public void         setMatchedSnippets(List<String> s){ this.matchedSnippets = s; }
    public void         addMatchedSnippet(String snippet){ this.matchedSnippets.add(snippet); }

    public double getConfidenceScore()                   { return confidenceScore; }
    public void   setConfidenceScore(double score)       { this.confidenceScore = score; }

    public long getLongFileSizeBytes()                   { return fileSizeBytes; }
    public void setFileSizeBytes(long size)              { this.fileSizeBytes = size; }

    public int  getWordCount()                           { return wordCount; }
    public void setWordCount(int count)                  { this.wordCount = count; }

    public String getErrorMessage()                      { return errorMessage; }
    public void   setErrorMessage(String msg)            { this.errorMessage = msg; }

    public boolean hasError()                            { return errorMessage != null; }

    /** Human-readable confidence percentage string */
    public String getConfidencePercent() {
        return String.format("%.0f%%", confidenceScore * 100);
    }

    @Override
    public String toString() {
        return String.format("%s → %s [%s] (%.0f%% confidence)",
            fileName, label != null ? label.getDisplayName() : "N/A",
            mode, confidenceScore * 100);
    }
}
package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of a labeling action, stored in the audit log.
 */
public class AuditEntry {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime timestamp;
    private String        fileName;
    private String        filePath;
    private SecurityLabel previousLabel;   // null if first time labeling
    private SecurityLabel newLabel;
    private String        mode;            // "MANUAL" or "AUTO"
    private String        user;
    private String        notes;

    public AuditEntry() {}

    public AuditEntry(String fileName, String filePath,
                      SecurityLabel previousLabel, SecurityLabel newLabel,
                      String mode, String user) {
        this.timestamp     = LocalDateTime.now();
        this.fileName      = fileName;
        this.filePath      = filePath;
        this.previousLabel = previousLabel;
        this.newLabel      = newLabel;
        this.mode          = mode;
        this.user          = user;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public LocalDateTime getTimestamp()                     { return timestamp; }
    public void          setTimestamp(LocalDateTime ts)     { this.timestamp = ts; }

    public String getFileName()                             { return fileName; }
    public void   setFileName(String fileName)              { this.fileName = fileName; }

    public String getFilePath()                             { return filePath; }
    public void   setFilePath(String filePath)              { this.filePath = filePath; }

    public SecurityLabel getPreviousLabel()                 { return previousLabel; }
    public void          setPreviousLabel(SecurityLabel l)  { this.previousLabel = l; }

    public SecurityLabel getNewLabel()                      { return newLabel; }
    public void          setNewLabel(SecurityLabel l)       { this.newLabel = l; }

    public String getMode()                                 { return mode; }
    public void   setMode(String mode)                      { this.mode = mode; }

    public String getUser()                                 { return user; }
    public void   setUser(String user)                      { this.user = user; }

    public String getNotes()                                { return notes; }
    public void   setNotes(String notes)                    { this.notes = notes; }

    /** Formatted timestamp string */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FMT) : "";
    }

    /** Displays the label change as an arrow, e.g. "None → Confidential" */
    public String getLabelChange() {
        String prev = previousLabel != null ? previousLabel.getDisplayName() : "None";
        String next = newLabel      != null ? newLabel.getDisplayName()      : "None";
        return prev + " → " + next;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %s",
            getFormattedTimestamp(), user, fileName, getLabelChange(), mode);
    }
}
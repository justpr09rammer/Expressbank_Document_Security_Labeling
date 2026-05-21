package org.example.model;

import java.util.UUID;

/**
 * Represents a single regex-based labeling rule.
 */
public class LabelRule {

    private String id;
    private String name;
    private String pattern;        // Regex pattern (case-insensitive by default)
    private SecurityLabel label;   // Label to assign when pattern matches
    private boolean enabled;
    private String description;
    private int priority;          // Higher priority rules evaluated first

    /** No-arg constructor required for Jackson deserialization */
    public LabelRule() {}

    public LabelRule(String name, String pattern, SecurityLabel label, String description) {
        this.id          = UUID.randomUUID().toString();
        this.name        = name;
        this.pattern     = pattern;
        this.label       = label;
        this.description = description;
        this.enabled     = true;
        this.priority    = label.getSeverity() * 10;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getId()                     { return id; }
    public void   setId(String id)            { this.id = id; }

    public String getName()                   { return name; }
    public void   setName(String name)        { this.name = name; }

    public String getPattern()                { return pattern; }
    public void   setPattern(String pattern)  { this.pattern = pattern; }

    public SecurityLabel getLabel()                    { return label; }
    public void          setLabel(SecurityLabel label) { this.label = label; }

    public boolean isEnabled()                { return enabled; }
    public void    setEnabled(boolean enabled){ this.enabled = enabled; }

    public String getDescription()                      { return description; }
    public void   setDescription(String description)    { this.description = description; }

    public int  getPriority()                 { return priority; }
    public void setPriority(int priority)     { this.priority = priority; }

    @Override
    public String toString() {
        return String.format("[%s] %s → %s", enabled ? "ON" : "OFF", name, label.getDisplayName());
    }
}
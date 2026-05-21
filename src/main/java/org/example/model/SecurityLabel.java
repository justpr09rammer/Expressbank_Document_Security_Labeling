package org.example.model;

import java.awt.Color;

/**
 * Represents the four security classification levels.
 * Labels are ordered by severity: PUBLIC < INTERNAL_ONLY < CONFIDENTIAL < RESTRICTED.
 */
public enum SecurityLabel {

    PUBLIC(
        "Public",
        "#27AE60",
        new Color(39, 174, 96),
        "Information that poses no harm if disclosed externally.",
        "Press releases, marketing materials, public product documentation, general announcements.",
        "Basic integrity checks. No special handling required.",
        1
    ),

    INTERNAL_ONLY(
        "Internal Only",
        "#2980B9",
        new Color(41, 128, 185),
        "For internal use only. Low risk if accidentally leaked, but not intended for public.",
        "Internal policies, employee handbooks, training documents, meeting notes.",
        "Access controls, authentication required. Do not share outside the organisation.",
        2
    ),

    CONFIDENTIAL(
        "Confidential",
        "#E67E22",
        new Color(230, 126, 34),
        "Sensitive information that could be harmful if exposed to unauthorised parties.",
        "Personal/financial records, HR data, legal documents, business strategies.",
        "Encryption at rest and in transit. Strict access controls. Audit logging required.",
        3
    ),

    RESTRICTED(
        "Restricted",
        "#C0392B",
        new Color(192, 57, 43),
        "The most sensitive classification. Severe organisational or legal impact if exposed.",
        "Trade secrets, classified project data, cryptographic keys, critical infrastructure info.",
        "Highest-level encryption and security monitoring. Need-to-know access only. Legal review required.",
        4
    );

    private final String displayName;
    private final String hexColor;
    private final Color awtColor;
    private final String definition;
    private final String examples;
    private final String securityMeasures;
    private final int severity;

    SecurityLabel(String displayName, String hexColor, Color awtColor,
                  String definition, String examples, String securityMeasures, int severity) {
        this.displayName   = displayName;
        this.hexColor      = hexColor;
        this.awtColor      = awtColor;
        this.definition    = definition;
        this.examples      = examples;
        this.securityMeasures = securityMeasures;
        this.severity      = severity;
    }

    public String getDisplayName()     { return displayName; }
    public String getHexColor()        { return hexColor; }
    public Color  getAwtColor()        { return awtColor; }
    public String getDefinition()      { return definition; }
    public String getExamples()        { return examples; }
    public String getSecurityMeasures(){ return securityMeasures; }
    public int    getSeverity()        { return severity; }

    /** Returns the higher-severity label. Useful when merging multiple rule matches. */
    public static SecurityLabel max(SecurityLabel a, SecurityLabel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.severity >= b.severity ? a : b;
    }

    @Override
    public String toString() { return displayName; }
}
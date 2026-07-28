package com.pronurse.onboarding.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Represents the type of onboarding document a nurse must submit.
 * These mirror the document categories shown on the nurse's mobile profile screen.
 */
public enum DocumentType {

    NURSING_CERTIFICATE("Nursing Certificate"),
    AADHAAR_CARD("Aadhaar Card"),
    PAN_CARD("PAN Card"),
    EXPERIENCE_CERTIFICATE("Experience Certificate"),
    PHOTO_ID("Photo ID"),
    DEGREE_CERTIFICATE("Degree Certificate"),
    OTHER("Other");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return this.name(); // Serialize as enum name (e.g., NURSING_CERTIFICATE)
    }

    @JsonCreator
    public static DocumentType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "DocumentType cannot be null or empty. Allowed values: " + Arrays.toString(DocumentType.values()));
        }
        try {
            return DocumentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid document type: '" + value + "'. Allowed values: " + Arrays.toString(DocumentType.values()));
        }
    }

    public String getReadableName() {
        return displayName;
    }
}

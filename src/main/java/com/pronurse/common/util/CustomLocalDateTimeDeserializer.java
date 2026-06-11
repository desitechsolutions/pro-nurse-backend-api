/*
package com.pronurse.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    // Supports all date  patterns
    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_INSTANT,                        // 2025-08-18T10:20:30Z
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,                // 2025-08-18T10:20:30
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"), // 2025-08-18T10:20:30.123
            DateTimeFormatter.ISO_LOCAL_DATE,                     // 2025-08-18
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),            // 18-08-2025
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),            // 08/18/2025
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),            // 18/08/2025
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),            // 2025/08/18
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),  // 18 Aug 2025
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)   // 18-Aug-2025
    );

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_INSTANT) {
                    // Special case: parse Instant -> LocalDateTime
                    Instant instant = Instant.parse(value);
                    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                }

                // If it's a pure date (no time), assume start of day
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE ||
                        formatter.toString().contains("d") && !value.contains("T") && value.split("[-/ ]").length == 3) {
                    LocalDate localDate = LocalDate.parse(value, formatter);
                    return localDate.atStartOfDay();
                }

                return LocalDateTime.parse(value, formatter);

            } catch (DateTimeParseException ignored) {
            }
        }

        throw new RuntimeException("Unsupported date format: " + value);
    }
}
*/

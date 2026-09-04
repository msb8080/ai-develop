package dev.rainbow.aidevelop.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }
}

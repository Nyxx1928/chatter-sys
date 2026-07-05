package org.example.chat.dto;

public record VerifyOtpResponse(
        boolean success,
        String message
) {}

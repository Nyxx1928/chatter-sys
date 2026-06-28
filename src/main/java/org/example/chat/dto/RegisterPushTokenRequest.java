package org.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPushTokenRequest {

    @NotBlank(message = "Push token is required")
    private String pushToken;

    @NotBlank(message = "Platform is required")
    private String platform;
}

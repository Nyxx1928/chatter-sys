package org.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnregisterPushTokenRequest {

    @NotBlank(message = "Push token is required")
    private String pushToken;
}

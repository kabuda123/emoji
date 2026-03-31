package com.company.emoji.user.dto;

import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @Size(max = 200) String reason,
        @Size(max = 32) String confirmText
) {
}
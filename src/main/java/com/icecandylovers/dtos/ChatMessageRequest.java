package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "A mensagem é obrigatória.")
        @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres.")
        String message
) {}

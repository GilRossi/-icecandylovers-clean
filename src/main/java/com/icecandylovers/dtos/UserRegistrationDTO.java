package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration. Only exposes username and password to prevent
 * mass assignment attacks (injecting id, role, or other entity fields).
 */
public class UserRegistrationDTO {

    @NotBlank(message = "Nome de usuario e obrigatorio")
    @Size(min = 3, max = 20, message = "Nome de usuario deve ter entre 3 e 20 caracteres")
    private String username;

    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String password;

    public UserRegistrationDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

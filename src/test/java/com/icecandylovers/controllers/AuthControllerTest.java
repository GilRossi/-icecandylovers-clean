package com.icecandylovers.controllers;

import com.icecandylovers.dtos.UserRegistrationDTO;
import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController controller;

    @Test
    void showRegisterForm_deveRetornarViewRegister() {
        Model model = new ConcurrentModel();
        String view = controller.showRegisterForm(model);
        assertEquals("register", view);
        assertTrue(model.containsAttribute("user"));
    }

    @Test
    void showLoginForm_deveRetornarViewLogin() {
        String view = controller.showLoginForm();
        assertEquals("login", view);
    }

    @Test
    void registerUser_deveRedirecionarParaLogin_quandoSucesso() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("testuser");
        dto.setPassword("senha123");
        BindingResult result = new BeanPropertyBindingResult(dto, "user");
        Model model = new ConcurrentModel();

        doNothing().when(userService).registerUser("testuser", "senha123");

        String view = controller.registerUser(dto, result, model);
        assertEquals("redirect:/login?registerSuccess", view);
        verify(userService).registerUser("testuser", "senha123");
    }

    @Test
    void registerUser_deveRetornarRegister_quandoDuplicado() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("existente");
        dto.setPassword("senha123");
        BindingResult result = new BeanPropertyBindingResult(dto, "user");
        Model model = new ConcurrentModel();

        doThrow(new DuplicateResourceException("Ja existe")).when(userService).registerUser(anyString(), anyString());

        String view = controller.registerUser(dto, result, model);
        assertEquals("register", view);
        assertTrue(model.containsAttribute("error"));
    }

    @Test
    void registerUser_deveRetornarRegister_quandoValidacaoFalha() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("");
        dto.setPassword("");
        BindingResult result = new BeanPropertyBindingResult(dto, "user");
        result.rejectValue("username", "NotBlank", "Username required");
        Model model = new ConcurrentModel();

        String view = controller.registerUser(dto, result, model);
        assertEquals("register", view);
        verify(userService, never()).registerUser(anyString(), anyString());
    }
}

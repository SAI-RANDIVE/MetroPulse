package com.metropulse.controller.web;

import com.metropulse.dto.form.RegistrationForm;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Sign-in page (the POST is handled by Spring Security) and commuter registration. */
@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("form") RegistrationForm form) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult binding, RedirectAttributes redirect) {
        if (!binding.hasFieldErrors("confirmPassword") && form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (binding.hasErrors()) {
            return "auth/register";
        }
        try {
            users.registerCommuter(form);
        } catch (MetroPulseException e) {
            binding.rejectValue("email", "duplicate", e.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("success", "Account created - please sign in.");
        return "redirect:/login";
    }
}

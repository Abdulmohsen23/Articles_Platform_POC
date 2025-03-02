package com.example.first.controllers;

import com.example.first.models.UserEntity;
import com.example.first.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController
{
    @Autowired
    private UserService userService;

    // Show registration form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new UserEntity());
        }
        return "users/register";
    }

    // Handle registration
    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserEntity user,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Please login.");
            return "redirect:/users/login?registered=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/register";
        }
    }

    // Show login form
    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String registered) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! Please login");
        }
        return "users/login";
    }

    // Handle login
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            // Try to find user by email
            UserEntity user = userService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if password matches
            if (password.equals(user.getPassword())) {
                // Store user in session
                session.setAttribute("loggedInUser", user);
                System.out.println("User logged in: " + user.getEmail()); // Debug log
                session.setAttribute("loggedOutUser", user);

                // Redirect based on role
                if (user.getRole() == UserEntity.Role.ADMIN) {
                    return "redirect:/articles/admin/pending";
                } else {
                    return "redirect:/articles/new";
                }
            } else {
                throw new RuntimeException("Invalid password");
            }
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage()); // Debug log
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Login failed: " + e.getMessage());
            return "redirect:/users/login";
        }
    }

    // Handle logout
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        // Clear the user from session
        session.removeAttribute("loggedInUser");
        // Invalidate the entire session
        session.invalidate();

        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully");
        return "redirect:/users/login?logout=true";
    }

}

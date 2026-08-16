package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String username = (String) request.getAttribute("authUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user_not_found"));
        }
        User u = userOpt.get();
        String locale = null;
        if (u.getLanguage() != null && !u.getLanguage().isBlank()) {
            locale = u.getLanguage();
            if (u.getRegion() != null && !u.getRegion().isBlank()) {
                locale = locale + "-" + u.getRegion();
            }
        }
        return ResponseEntity.ok(Map.of(
                "userId", u.getId(),
                "username", u.getUsername(),
                "nickname", u.getNickname(),
                "email", u.getEmail(),
                "locale", locale
        ));
    }
}

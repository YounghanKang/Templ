package com.example.demo.controller;

import com.example.demo.dto.AuthDto;
import com.example.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody AuthDto.SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> googleLogin(@RequestBody AuthDto.GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/check-id")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestBody AuthDto.CheckUsernameRequest request) {
        return ResponseEntity.ok(Map.of("available", authService.checkUsernameAvailable(request.getUsername())));
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(@RequestBody AuthDto.SendCodeRequest request) {
        return ResponseEntity.ok(Map.of("status", authService.sendCode(request.getEmail())));
    }

    @PostMapping("/confirm-code")
    public ResponseEntity<Map<String, Boolean>> confirmCode(@RequestBody AuthDto.ConfirmCodeRequest request) {
        return ResponseEntity.ok(Map.of("verified", authService.confirmCode(request.getEmail(), request.getCode())));
    }
}

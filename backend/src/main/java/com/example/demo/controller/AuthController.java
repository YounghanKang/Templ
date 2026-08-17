package com.example.demo.controller;

import com.example.demo.dto.AuthDto;
import com.example.demo.service.AuthService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
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

    @GetMapping("/google/start")
    public ResponseEntity<Void> googleStart(@RequestParam(value = "redirect", required = false) String frontendRedirect,
                                           HttpServletRequest req) {
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        if (clientId == null || clientId.isBlank()) {
            // If client id not set, inform caller
            return ResponseEntity.badRequest().build();
        }

        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String callback = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(host)
                .port(port)
                .path("/api/auth/google/callback")
                .build()
                .toUriString();

        UriComponentsBuilder ub = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("accounts.google.com")
                .path("/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("redirect_uri", callback)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent");

        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            ub.queryParam("state", frontendRedirect);
        }

        URI redirectUri = URI.create(ub.build().toUriString());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);
        return ResponseEntity.status(302).headers(headers).build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam(value = "code", required = false) String code,
                                            @RequestParam(value = "state", required = false) String state,
                                            HttpServletRequest req) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing code"));
        }

        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "google client id/secret not configured"));
        }

        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String callback = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(host)
                .port(port)
                .path("/api/auth/google/callback")
                .build()
                .toUriString();

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", callback);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        Map<String, Object> tokenResponse;
        try {
            tokenResponse = rest.postForObject("https://oauth2.googleapis.com/token", request, Map.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "token_exchange_failed", "detail", e.getMessage()));
        }

        if (tokenResponse == null || !tokenResponse.containsKey("id_token")) {
            return ResponseEntity.status(500).body(Map.of("error", "no_id_token_in_response", "resp", tokenResponse));
        }

        String idToken = tokenResponse.get("id_token").toString();
        // reuse existing googleLogin flow
        AuthDto.GoogleLoginRequest glr = AuthDto.GoogleLoginRequest.builder().accessToken(idToken).build();
        AuthDto.AuthResponse authResp = authService.googleLogin(glr);

        if (state != null && !state.isBlank()) {
            // redirect back to frontend with backend JWT as query param
            String redirectTo = UriComponentsBuilder.fromUriString(state)
                    .queryParam("accessToken", authResp.getAccessToken())
                    .build().toUriString();
            HttpHeaders rh = new HttpHeaders();
            rh.setLocation(URI.create(redirectTo));
            return ResponseEntity.status(302).headers(rh).build();
        }

        return ResponseEntity.ok(authResp);
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

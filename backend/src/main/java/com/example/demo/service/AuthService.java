package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.dto.AuthDto;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.GoogleTokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.googleTokenVerifier = googleTokenVerifier;

        // seed demo users if absent
        if (!userRepository.existsByUsername("admin")) {
            User u = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .nickname("Admin")
                    .build();
            userRepository.save(u);
        }
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getUsername());

        return AuthDto.AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    public void signup(AuthDto.SignupRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signup payload is invalid");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already taken");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already used");
        }

        User u = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail() == null ? "" : request.getEmail())
                .nickname(request.getNickname() == null ? request.getUsername() : request.getNickname())
                .build();
        userRepository.save(u);
    }

    public boolean checkUsernameAvailable(String username) {
        return username != null && !username.isBlank() && !userRepository.existsByUsername(username);
    }

    // keep verification code behavior in-memory (could be moved to DB/email later)
    private final java.util.Map<String, String> verificationCodes = new java.util.concurrent.ConcurrentHashMap<>();

    public String sendCode(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        String code = "1234";
        verificationCodes.put(email.trim().toLowerCase(), code);
        return code;
    }

    public boolean confirmCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String expected = verificationCodes.get(email.trim().toLowerCase());
        return expected != null && expected.equals(code);
    }

    public AuthDto.AuthResponse googleLogin(AuthDto.GoogleLoginRequest request) {
        if (request == null || request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accessToken is required");
        }

        // Treat accessToken as Google ID token (id_token). Verify when GOOGLE_CLIENT_ID is set.
        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = googleTokenVerifier.verify(request.getAccessToken());

        String email;
        String givenName = null;
        String localeStr = null;

        if (googleTokenVerifier.isEnabled()) {
            // verification enabled: reject if token invalid
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid google id_token");
            }
            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
            email = payload.getEmail();
            Object nameObj = payload.get("name");
            if (nameObj != null) givenName = nameObj.toString();
            Object localeObj = payload.get("locale");
            if (localeObj != null) localeStr = localeObj.toString();
        } else {
            // verification disabled — fallback demo behavior
            email = "google_" + request.getAccessToken().substring(0, Math.min(8, request.getAccessToken().length())) + "@example.com";
            givenName = "Google User";
        }

        String googleUsername = email.split("@")[0];
        Optional<User> userOpt = userRepository.findByUsername(googleUsername);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = User.builder()
                    .username(googleUsername)
                    .password(passwordEncoder.encode("oauth"))
                    .email(email)
                    .nickname(givenName == null ? googleUsername : givenName)
                    .build();
            user = userRepository.save(user);
        }

        // persist locale into user's language/region fields if available
        if (localeStr != null && !localeStr.isBlank()) {
            String lang = localeStr;
            String region = null;
            if (localeStr.contains("-") || localeStr.contains("_")) {
                String sep = localeStr.contains("-") ? "-" : "_";
                String[] parts = localeStr.split(sep);
                lang = parts[0];
                if (parts.length > 1) region = parts[1];
            }
            user.setLanguage(lang);
            user.setRegion(region);
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return AuthDto.AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .locale(localeStr)
                .build();
    }
}

package com.example.demo.controller;

import com.example.demo.dto.UserDto;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/profile")
    public ResponseEntity<UserDto.ProfileDto> getProfile() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @PatchMapping("/users/profile")
    public ResponseEntity<UserDto.ProfileDto> updateProfile(@RequestBody UserDto.ProfileDto request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @GetMapping("/teams/{teamId}/integrations")
    public ResponseEntity<UserDto.IntegrationSettingsDto> getIntegrations(@PathVariable String teamId) {
        return ResponseEntity.ok(userService.getIntegrations());
    }

    @PatchMapping("/teams/{teamId}/integrations")
    public ResponseEntity<UserDto.IntegrationSettingsDto> updateIntegrations(@PathVariable String teamId,
                                                                            @RequestBody UserDto.IntegrationSettingsDto request) {
        return ResponseEntity.ok(userService.updateIntegrations(request));
    }
}

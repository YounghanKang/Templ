package com.example.demo.controller;

import com.example.demo.dto.UserDto;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<UserDto.ProfileDto> getProfile(HttpServletRequest request) {
        String username = (String) request.getAttribute("authUser");
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @PatchMapping("/users/profile")
    public ResponseEntity<UserDto.ProfileDto> updateProfile(@RequestBody UserDto.ProfileDto profileRequest,
                                                            HttpServletRequest request) {
        String username = (String) request.getAttribute("authUser");
        return ResponseEntity.ok(userService.updateProfile(username, profileRequest));
    }
}

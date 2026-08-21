package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.dto.UserDto;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDto.ProfileDto updateProfile(String username, UserDto.ProfileDto request) {
        if (username != null && !username.isBlank()) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && request != null) {
                if (request.getRegion() != null) user.setRegion(request.getRegion());
                if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
                userRepository.save(user);
                return new UserDto.ProfileDto(
                        user.getRegion() != null ? user.getRegion() : "대한민국 · 서울",
                        user.getLanguage() != null ? user.getLanguage() : "한국어"
                );
            }
        }
        return request != null ? request : new UserDto.ProfileDto("대한민국 · 서울", "한국어");
    }

    public UserDto.ProfileDto getProfile(String username) {
        if (username != null && !username.isBlank()) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return new UserDto.ProfileDto(
                        user.getRegion() != null && !user.getRegion().isBlank() ? user.getRegion() : "대한민국 · 서울",
                        user.getLanguage() != null && !user.getLanguage().isBlank() ? user.getLanguage() : "한국어"
                );
            }
        }
        return new UserDto.ProfileDto("대한민국 · 서울", "한국어");
    }

    // Overload for backward compatibility / tests
    public UserDto.ProfileDto updateProfile(UserDto.ProfileDto request) {
        return updateProfile(null, request);
    }

    public UserDto.ProfileDto getProfile() {
        return getProfile(null);
    }
}

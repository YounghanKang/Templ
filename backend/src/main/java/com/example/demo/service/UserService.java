package com.example.demo.service;

import com.example.demo.dto.UserDto;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserDto.ProfileDto profile = new UserDto.ProfileDto("대한민국 · 서울", "한국어");
    private UserDto.IntegrationSettingsDto integrations = UserDto.IntegrationSettingsDto.builder()
            .slack(new UserDto.AccountDto(false, ""))
            .github(new UserDto.AccountDto(false, ""))
            .build();

    public UserDto.ProfileDto updateProfile(UserDto.ProfileDto request) {
        if (request != null) {
            this.profile = request;
        }
        return this.profile;
    }

    public UserDto.IntegrationSettingsDto updateIntegrations(UserDto.IntegrationSettingsDto request) {
        if (request != null) {
            this.integrations = request;
        }
        return this.integrations;
    }

    public UserDto.ProfileDto getProfile() {
        return this.profile;
    }

    public UserDto.IntegrationSettingsDto getIntegrations() {
        return this.integrations;
    }
}

package com.example.demo.service;

import com.example.demo.dto.UserDto;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserDto.ProfileDto profile = new UserDto.ProfileDto("대한민국 · 서울", "한국어");

    public UserDto.ProfileDto updateProfile(UserDto.ProfileDto request) {
        if (request != null) {
            this.profile = request;
        }
        return this.profile;
    }



    public UserDto.ProfileDto getProfile() {
        return this.profile;
    }

}

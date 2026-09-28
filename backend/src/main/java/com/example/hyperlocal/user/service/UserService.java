package com.example.hyperlocal.user.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.user.dto.UpdateProfileRequest;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.entity.UserStatus;
import com.example.hyperlocal.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        user.setName(req.getName().trim());
        if (req.getProfileImageUrl() != null) {
            user.setProfileImageUrl(req.getProfileImageUrl().trim());
        }
        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }
}

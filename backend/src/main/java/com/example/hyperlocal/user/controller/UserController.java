package com.example.hyperlocal.user.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.user.dto.UpdateProfileRequest;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = userService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req) {
        UserDto user = userService.updateProfile(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Account deactivated successfully")));
    }
}

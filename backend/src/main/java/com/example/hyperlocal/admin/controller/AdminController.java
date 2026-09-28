package com.example.hyperlocal.admin.controller;

import com.example.hyperlocal.admin.dto.AdminDashboardMetricsDto;
import com.example.hyperlocal.admin.dto.UserStatusUpdateRequest;
import com.example.hyperlocal.admin.service.AdminService;
import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardMetricsDto>> getDashboardMetrics() {
        AdminDashboardMetricsDto metrics = adminService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<Page<VendorDto>>> getVendors(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VendorDto> vendors = adminService.getVendors(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(vendors));
    }

    @PostMapping("/vendors/{id}/approve")
    public ResponseEntity<ApiResponse<VendorDto>> approveVendor(@PathVariable Long id) {
        VendorDto vendor = adminService.approveVendor(id);
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }

    @PostMapping("/vendors/{id}/suspend")
    public ResponseEntity<ApiResponse<VendorDto>> suspendVendor(@PathVariable Long id) {
        VendorDto vendor = adminService.suspendVendor(id);
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserDto> users = adminService.getUsers(role, q, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest req) {
        UserDto user = adminService.updateUserStatus(id, req.getStatus());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}

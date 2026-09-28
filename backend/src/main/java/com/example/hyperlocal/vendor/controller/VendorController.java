package com.example.hyperlocal.vendor.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.vendor.dto.StoreStatusUpdateRequest;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.dto.VendorRegistrationRequest;
import com.example.hyperlocal.vendor.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // Public discovery endpoints
    @GetMapping("/vendors/nearby")
    public ResponseEntity<ApiResponse<List<VendorDto>>> getNearbyVendors(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "distance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VendorDto> vendorPage = vendorService.getNearbyVendors(lat, lng, sort, page, size);
        Map<String, Object> meta = Map.of(
                "page", vendorPage.getNumber(),
                "size", vendorPage.getSize(),
                "totalElements", vendorPage.getTotalElements(),
                "totalPages", vendorPage.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(vendorPage.getContent(), meta));
    }

    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<ApiResponse<VendorDto>> getVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        VendorDto vendor = vendorService.getVendorById(vendorId, lat, lng);
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }

    @GetMapping("/vendors/{vendorId}/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVendorAvailability(@PathVariable Long vendorId) {
        VendorDto vendor = vendorService.getVendorById(vendorId, null, null);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "vendorId", vendorId,
                "isOpen", vendor.getIsOpen(),
                "status", vendor.getStatus()
        )));
    }

    @GetMapping("/vendors/search")
    public ResponseEntity<ApiResponse<List<VendorDto>>> searchVendors(
            @RequestParam String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        List<VendorDto> results = vendorService.searchVendors(q, lat, lng);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // Vendor operator console endpoints
    @PostMapping("/vendor/register")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorDto>> registerStore(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VendorRegistrationRequest req) {
        VendorDto vendor = vendorService.registerStore(principal.getId(), req);
        return new ResponseEntity<>(ApiResponse.success(vendor), HttpStatus.CREATED);
    }

    @GetMapping("/vendor/store")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorDto>> getMyStore(@AuthenticationPrincipal UserPrincipal principal) {
        VendorDto vendor = vendorService.getStoreByOwner(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }

    @PatchMapping("/vendor/store/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorDto>> updateStoreStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody StoreStatusUpdateRequest req) {
        VendorDto vendor = vendorService.updateStoreStatus(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }
}

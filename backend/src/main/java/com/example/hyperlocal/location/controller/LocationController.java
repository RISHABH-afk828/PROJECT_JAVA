package com.example.hyperlocal.location.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.location.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {
        Map<String, Object> result = locationService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchLocations(@RequestParam String q) {
        List<Map<String, Object>> results = locationService.searchLocations(q);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/serviceability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkServiceability(
            @RequestParam double lat,
            @RequestParam double lng) {
        Map<String, Object> result = locationService.checkServiceability(lat, lng);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
